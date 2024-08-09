import java.io.*;
import java.net.*;

public class Server {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor principal escuchando en el puerto " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado desde " + clientSocket.getInetAddress());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor principal: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())) {

            System.out.println("Esperando dato del cliente...");

            int data = in.readInt();
            System.out.println("Dato recibido del cliente " + data);

            int result = distributeTasks(data);
            System.out.println("Resultado total calculado: " + result);
            out.writeInt(result);
            out.flush();
            System.out.println("Resultado enviado al cliente.");
        } catch (IOException e) {
            System.err.println("Error en el manejador del cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int distributeTasks(int data) {
        int total = 0;
        int[] resultados = new int[4]; // Array para almacenar resultados parciales de hasta 4 workers.
        System.out.println("Distribuyendo tareas a los workers...");

        for (int i = 0; i < 4; i++) {
            int port = 5001 + i;
            System.out.println("Verificando disponibilidad del Worker en el puerto " + port);

            if (isWorkerAvailable(port)) {
                System.out.println("Worker en puerto " + port + " disponible. Intentando conexión...");
                int partialResult = tryWorkerConnection(port, data);
                resultados[i] = partialResult;
                total += partialResult;
                System.out.println("Resultado parcial del Worker en puerto " + port + ": " + partialResult);
            } else {
                System.out.println("Saltando Worker en puerto " + port + " debido a que no está disponible.");
            }
        }


        // Si algún resultado no fue calculado (es 0), redistribuir la tarea.
        for (int i = 0; i < resultados.length; i++) {
            if (resultados[i] == 0) {
                System.out.println("Redistribuyendo la tarea del Worker en puerto " + (5001 + i) + " a otro Worker.");
                for (int j = 0; j < resultados.length; j++) {
                    if (resultados[j] != 0) {
                        // Asumimos que un Worker funcional se encarga de la tarea.
                        int backupResult = tryWorkerConnection(5001 + j, data);
                        total += backupResult;
                        System.out.println("Tarea asumida por Worker en puerto " + (5001 + j) + ", resultado adicional: " + backupResult);
                        break; // Rompemos el bucle después de asignar la tarea de respaldo.
                    }
                }
            }
        }

        System.out.println("Resultado total después de todas las tareas: " + total);
        return total;
    }
    /*
    private int distributeTasks(int data) {
        int total = 0;
        boolean[] operacionesAsignadas = new boolean[4];

        for (int i = 0; i < 4; i++) {
            int port = 5001 + i;
            if (isWorkerAvailable(port)) {
                int partialResult = tryWorkerConnection(port, data, i, operacionesAsignadas);
                total += partialResult;
            } else {
                System.out.println("Worker en puerto " + port + " no disponible.");
            }
        }

        // Si alguna operación no fue asignada, reasignarla
        for (int i = 0; i < 4; i++) {
            if (!operacionesAsignadas[i]) {
                System.out.println("Reasignando operación " + i + " debido a falla.");
                total += tryFallbackConnection(data, i);
            }
        }

        return total;
    }

     */

    private boolean isWorkerAvailable(int port) {
        try (Socket workerSocket = new Socket("localhost", port);
             ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream())) {

            System.out.println("Enviando ping al Worker en puerto " + port);

            out.writeUTF("ping");
            out.flush();

            String response = in.readUTF();
            System.out.println("Respuesta del ping: " + response);
            return "pong".equals(response);

        } catch (IOException e) {
            System.err.println("Worker en el puerto " + port + " no disponible." + e.getMessage());
            return false;
        }
    }


    private int tryWorkerConnection(int port, int data) {
        int retries = 3; // Número de intentos
        int partialResult = 0;

        while (retries > 0) {
            try (Socket workerSocket = new Socket("localhost", port);
                 ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream())) {

                System.out.println("Conectando al Worker en el puerto " + port);

                System.out.println("Enviando tipo de solicitud al Worker en puerto " + port);
                out.writeUTF("process");
                out.flush();

                // Enviar datos al worker
                System.out.println("Enviando datos al Worker en puerto " + port + ": " + data);
                out.writeInt(data);
                out.flush();

                // Leer resultado del worker
                System.out.println("Esperando resultado del Worker en puerto " + port);
                partialResult = in.readInt();
                System.out.println("Resultado parcial recibido del Worker en puerto " + port + ": " + partialResult);

                break; // Salir del bucle si la conexión fue exitosa
            } catch (IOException e) {
                retries--;
                System.err.println("Error en la comunicación con el Worker en el puerto " + port + ". Intentos restantes: " + retries);
                try {
                    Thread.sleep(1000); // Espera un segundo antes de reintentar
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }

        if (retries == 0) {
            System.out.println("Fallo permanente en el Worker en puerto " + port + ". Usando valor por defecto.");
            // Aquí puedes decidir qué hacer si se agotaron los intentos
        }

        return partialResult;
    }


    /*
    private int tryWorkerConnection(int port, int data, int operationIndex, boolean[] operacionesAsignadas) {
        int retries = 3;
        int partialResult = 0;

        while (retries > 0) {
            try (Socket workerSocket = new Socket("localhost", port);
                 ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream())) {

                System.out.println("Conectando al Worker en el puerto " + port);

                out.writeInt(operationIndex); // Asignar la operación
                out.writeInt(data);
                out.flush();

                partialResult = in.readInt();
                System.out.println("Resultado parcial recibido del Worker en puerto " + port + ": " + partialResult);
                operacionesAsignadas[operationIndex] = true;
                break;
            } catch (IOException e) {
                retries--;
                System.err.println("Error en la comunicación con el Worker en el puerto " + port + ". Intentos restantes: " + retries);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }

        return partialResult;
    }

    private int tryFallbackConnection(int data, int operationIndex) {
        // Intentar con cualquier worker disponible
        for (int i = 0; i < 4; i++) {
            int port = 5001 + i;
            if (isWorkerAvailable(port)) {
                System.out.println("Asignando operación " + operationIndex + " al Worker en puerto " + port);
                return tryWorkerConnection(port, data, operationIndex, new boolean[4]);
            }
        }
        // Si no hay workers disponibles, devuelve un valor por defecto o lanza una excepción
        System.err.println("No hay workers disponibles para la operación " + operationIndex);
        return 0;
    }

    private boolean isWorkerAvailable(int port) {
        try (Socket workerSocket = new Socket("localhost", port);
             ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream())) {

            System.out.println("Enviando ping al Worker en puerto " + port);

            out.writeUTF("ping");
            out.flush();

            String response = in.readUTF();
            System.out.println("Respuesta del ping: " + response);
            return "pong".equals(response);

        } catch (IOException e) {
            System.err.println("Worker en el puerto " + port + " no disponible: " + e.getMessage());
            return false;
        }
    }

*/
}


