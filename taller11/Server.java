import java.io.*;
import java.net.*;

public class Server {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor principal escuchando en el puerto " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
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

            int data = in.readInt();
            System.out.println("Dato recibido: " + data);

            int result = distributeTasks(data);
            out.writeInt(result);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int distributeTasks(int data) {
        int[] partialResults = new int[4];
        int total = 0;

        for (int i = 0; i < 4; i++) {
            try (Socket workerSocket = new Socket("localhost", 5001 + i);
                 ObjectOutputStream out = new ObjectOutputStream(workerSocket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(workerSocket.getInputStream())) {

                // Enviar datos al worker
                out.writeInt(data);
                out.flush();

                // Leer resultado del worker
                partialResults[i] = in.readInt();
                total += partialResults[i];
            } catch (IOException e) {
                System.err.println("Error en la comunicación con el worker en el puerto " + (5001 + i));
                e.printStackTrace();
            }
        }
        return total;
    }
}
