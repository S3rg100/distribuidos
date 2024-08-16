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
            System.out.println("Dato recibido del cliente: " + data);

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
                int partialResult = tryWorkerConnection(port, "process", data);  // Pasamos la operación y el dato
                resultados[i] = partialResult;
                total += partialResult;
                System.out.println("Resultado parcial del Worker en puerto " + port + ": " + partialResult);
            } else {
                System.out.println("Saltando Worker en puerto " + port + " debido a que no está disponible.");
            }
        }

        // Redistribuir tareas si es necesario
        for (int i = 0; i < resultados.length; i++) {
            if (resultados[i] == 0) {
                System.out.println("Redistribuyendo la tarea del Worker en puerto " + (5001 + i) + " a otro Worker.");
                for (int j = 0; j < resultados.length; j++) {
                    if (resultados[j] != 0) {
                        int backupResult = tryWorkerConnection(5001 + j, "process", data);  // Operación y dato
                        total += backupResult;
                        System.out.println("Tarea asumida por Worker en puerto " + (5001 + j) + ", resultado adicional: " + backupResult);
                        break;
                    }
                }
            }
        }

        System.out.println("Resultado total después de todas las tareas: " + total);
        return total;
    }

    private boolean isWorkerAvailable(int port) {
        try (Socket workerSocket = new Socket("localhost", port);
             ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream())) {

            // Enviar una solicitud de prueba (por ejemplo, ping)
            out.writeUTF("ping");
            out.flush();

            // Leer la respuesta
            String response = in.readUTF();
            return "pong".equals(response);

        } catch (IOException e) {
            System.err.println("Worker en el puerto " + port + " no disponible o no funcional: " + e.getMessage());
            return false;
        }
    }

    private int tryWorkerConnection(int port, String operation, int data) {
        int retries = 3;
        int partialResult = 0;

        while (retries > 0) {
            try (Socket workerSocket = new Socket("localhost", port);
                 ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream())) {

                System.out.println("Conectando al Worker en el puerto " + port);

                // Enviar tipo de operación
                out.writeUTF(operation);
                out.flush();

                // Enviar datos al worker
                System.out.println("Enviando operación: " + operation + " y datos: " + data + " al Worker en puerto " + port);
                out.writeInt(data);
                out.flush();

                // Leer resultado del worker
                partialResult = in.readInt();
                System.out.println("Resultado parcial recibido del Worker en puerto " + port + ": " + partialResult);

                break; // Salir del bucle si la conexión fue exitosa
            } catch (IOException e) {
                retries--;
                System.err.println("Error en la comunicación con el Worker en el puerto " + port + ". Intentos restantes: " + retries);
                try {
                    Thread.sleep(1000); // Esperar un segundo antes de reintentar
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }

        if (retries == 0) {
            System.out.println("Fallo permanente en el Worker en puerto " + port + ". Usando valor por defecto.");
        }

        return partialResult;
    }
}
