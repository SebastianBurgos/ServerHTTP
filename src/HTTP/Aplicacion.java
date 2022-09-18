package HTTP;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Aplicacion {
	public static final int PORT = 3400;

    public static void main(String[] args) {
        ServerSocket server_side_socket = getServerSocket();

        //Se verifica si el serversocket está conectado correctamente y no esta cerrado
        while (server_side_socket.isBound() && !server_side_socket.isClosed()) {
            //Se aceptan comunicaciones y se crean hilos para los clientes
            Socket listener = aceptarComunicacion(server_side_socket);

            //Se crea un hilo en el se ingresa un nuevo objeto de tipo SERVERHTTP que extiende de
            //Runnable y se corre el hilo
            Thread hilo = new Thread(new ServerHTTP(listener));
            hilo.start();
        }
        //Al final cuando el hilo muere se utiliza este metodo para cerrar los canales de
        cerrarCanales(server_side_socket);
    }

    /**
     * Metodo para obtener un nuevo socket de tipo ServerSocket
     * @return
     */
    private static ServerSocket getServerSocket() {
        ServerSocket server_socket = null;
        try {
            server_socket = new ServerSocket(PORT);
        } catch (IOException e) {
            System.out.println(e);
        }
        return server_socket;
    }

    /**
     * Metodo en que se abre la conexion a los clientes, por medio del metodo .accept() de la clase ServerSocket
     * @param server_socket
     * @return
     */
    private static Socket aceptarComunicacion(ServerSocket server_socket) {
        Socket socket = null;
        try {
        	System.out.println("Server HTTP is running on port: " + PORT);
            socket = server_socket.accept();
        } catch (IOException e) {
            System.out.println(e);
        }
        return socket;
    }

    /**
     * Este metodo cierra el canal del ServerSocket para que el servidor pare la conexión
     * @param server_socket
     */
    private static void cerrarCanales(ServerSocket server_socket) {
        try {
            server_socket.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }
}
