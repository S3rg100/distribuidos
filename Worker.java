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

            int result = performOperation(requestType, data);
            System.out.println("Resultado de la operación: " + result);
            out.writeInt(result);
            out.flush();
            System.out.println("Resultado enviado al servidor.");
        } catch (IOException e) {
            System.err.println("Error en el WorkerHandler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int performOperation(String operation, int data) {
        switch (operation) {
            case "multiply":
                System.out.println("Worker en puerto " + socket.getLocalPort() + " multiplicando datos por 2.");
                return data * 2; // Ejemplo de multiplicación
            case "add":
                System.out.println("Worker en puerto " + socket.getLocalPort() + " sumando 10 a los datos.");
                return data + 10; // Ejemplo de suma
            case "divide":
                System.out.println("Worker en puerto " + socket.getLocalPort() + " dividiendo los datos por 2.");
                return data / 2; // Ejemplo de división
            case "subtract":
                System.out.println("Worker en puerto " + socket.getLocalPort() + " restando 10 a los datos.");
                return data - 10; // Ejemplo de resta
            default:
                System.out.println("Operación por defecto.");
                return data * 1; // Operación por defecto
        }
    }
}
