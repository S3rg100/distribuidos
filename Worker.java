import java.io.*;
import java.net.*;

public class Worker {
    final private int port;

    public Worker(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Worker escuchando en el puerto " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado al Worker en el puerto " + port);
                new WorkerHandler(socket).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el Worker en el puerto " + port + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Por favor, proporciona un número de puerto.");
            System.exit(1);
        }
        int port = Integer.parseInt(args[0]);
        Worker worker = new Worker(port);
        worker.start();
    }
}

class WorkerHandler extends Thread {
    final private Socket socket;

    public WorkerHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            System.out.println("Esperando tipo de solicitud...");

            String requestType = in.readUTF(); // Leemos el tipo de solicitud
            System.out.println("Tipo de solicitud recibida: " + requestType);

            if ("ping".equals(requestType)) {
                System.out.println("Recibido 'ping'. Enviando 'pong'...");
                out.writeUTF("pong");
                out.flush();
                System.out.println("'pong' enviado. Finalizando conexión.");
                return; // Finalizamos el handler para este ping
            }
            System.out.println("Esperando datos para la operación...");
            int data = in.readInt(); // Continuamos con el flujo normal para operaciones
            System.out.println("Datos recibidos: " + data);

            int result = performOperation(data);
            System.out.println("Resultado de la operación: " + result);
            out.writeInt(result);
            out.flush();
            System.out.println("Resultado enviado al servidor.");
        } catch (IOException e) {
            System.err.println("Error en el WorkerHandler: " + e.getMessage());
            e.printStackTrace();
        }
    }
    //Cuando falla un worker, los otros pueden resolver pero hacen su propia operacion OJO
    private int performOperation(int data) {
        // Diferentes operaciones según el Worker
        switch(socket.getLocalPort()) {
            case 5001:
                System.out.println("Worker en puerto 5001 multiplicando datos por 2.");
                return data * 2; // Ejemplo de multiplicación
            case 5002:
                System.out.println("Worker en puerto 5002 sumando 10 a los datos.");
                return data + 10; // Ejemplo de suma
            case 5003:
                System.out.println("Worker en puerto 5003 dividiendo los datos por 2.");
                return data / 2; // Ejemplo de división
            case 5004:
                System.out.println("Worker en puerto 5004 restando 10 a los datos.");
                return data - 10; // Ejemplo de resta
            default:
                System.out.println("Operación por defecto.");
                return data * 1; // Operación por defecto
        }
    }
}

/*
class WorkerHandler extends Thread {
    final private Socket socket;

    public WorkerHandler(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            System.out.println("Esperando tipo de solicitud...");

            String requestType = in.readUTF(); // Leemos el tipo de solicitud
            System.out.println("Tipo de solicitud recibida: " + requestType);

            if ("ping".equals(requestType)) {
                System.out.println("Recibido 'ping'. Enviando 'pong'...");
                out.writeUTF("pong");
                out.flush();
                System.out.println("'pong' enviado. Finalizando conexión.");
                return; // Finalizamos el handler para este ping
            }

            System.out.println("Esperando índice de operación...");
            int operationIndex = in.readInt(); // Leer el índice de operación

            System.out.println("Esperando datos para la operación...");
            int data = in.readInt(); // Continuamos con el flujo normal para operaciones
            System.out.println("Datos recibidos: " + data);

            int result = performOperation(operationIndex, data);
            System.out.println("Resultado de la operación: " + result);
            out.writeInt(result);
            out.flush();
            System.out.println("Resultado enviado al servidor.");
        } catch (IOException e) {
            System.err.println("Error en el WorkerHandler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int performOperation(int operationIndex, int data) {
        switch (operationIndex) {
            case 0:
                System.out.println("Multiplicando datos por 2.");
                return data * 2;
            case 1:
                System.out.println("Sumando 10 a los datos.");
                return data + 10;
            case 2:
                System.out.println("Dividiendo los datos por 2.");
                return data / 2;
            case 3:
                System.out.println("Restando 10 a los datos.");
                return data - 10;
            default:
                System.out.println("Operación por defecto.");
                return data;
        }
    }
}

 */
