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
                new WorkerHandler(socket).start();
            }
        } catch (IOException e) {
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

            int data = in.readInt();
            System.out.println("Worker " + socket.getLocalPort() + " recibió: " + data);

            int result = performOperation(data);
            out.writeInt(result);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int performOperation(int data) {
        return data * 2; // Ejemplo de operación
    }
}
