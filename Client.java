import java.io.*;
import java.net.*;

public class Client {
    private static final int PORT = 5000;
    private static final String SERVER_ADDRESS = "localhost";
    /*
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            int data = 50; // Dato de ejemplo a enviar
            out.writeInt(data);
            out.flush();

            int result = in.readInt();
            System.out.println("Resultado recibido del servidor: " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Por favor, proporciona un número como argumento.");
            System.exit(1);
        }

        int data = Integer.parseInt(args[0]);

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeInt(data);
            out.flush();

            int result = in.readInt();
            System.out.println("Resultado recibido del servidor: " + result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
