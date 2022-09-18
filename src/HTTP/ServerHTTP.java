package HTTP;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

//Se realizó como un hilo para que no se cierren los canales hasta que el cliente se desconecte
//Y para que pueda enviar varias solicitudes sin problema
public class ServerHTTP implements Runnable {

    private Socket listener;

    /**
     * Metodo constructor
     * @param listener
     */
    public ServerHTTP(Socket listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
    	//Se crea un bufferedreader para obtener el mensaje de solicitud del cliente
        BufferedReader fromNetwork = getBufferedReader(listener);
        //Se crea un outputstream para enviar los datos al cliente
        OutputStream toNetwork = getOutputStream(listener);
        //Se inicializa el mensaje de solicitud vacío
        String mensaje_solicitud;

        //Recibe la cabecera del cliente (solo la primera linea que es donde se encuentra el nombre del archivo solicitado)
        String mensaje = recibirPeticion(fromNetwork);

        while (mensaje != null) {
        	//Mientras el mensaje no sea nulo entonces se va a proceder a obtener el resto del mensaje
        	//Todas las lineas que faltan
            mensaje_solicitud = mensaje+"\r\n";
            String linea = mensaje_solicitud+"\r\n";
            //Hasta que se llegue al final del mensaje que es un salto de linea vacio "" y se va concatenando al mensaje de solicitud
            while (!linea.equals("")) {
                linea = recibirPeticion(fromNetwork);
                mensaje_solicitud += linea+"\r\n";
            }
            //Imprimimos esto en consola para verificar el mensaje de solicitud completo
    	    System.out.println("\n[Server]Mensaje de Solicitud From client: \n"+ mensaje_solicitud);

    	    //Se crea la respuesta en un array de bytes para mandar por el outputstream
            byte[] respuesta = componerRespuesta(mensaje);

            //Se envía el mensaje completo al outputstream que es el toNetwork
            enviarRespuesta(toNetwork, respuesta);

            //Lee las otras peticiones si las necesita el cliente
            mensaje = recibirPeticion(fromNetwork);
        }

        //Cuabdo el cliente se desconecta, el hilo lee un null y entonces se muere y se cierran los canales
        cerrarCanales(fromNetwork, toNetwork);
    }

    /**
     * Metodo que compone una respuesta HTTP desde el servidor
     * @param mensaje
     * @return
     */
    private static byte[] componerRespuesta(String mensaje) {
    	//Construye la cabecera del mensaje
        String date = obtenerFecha();
        String finLinea = "\r\n";
        String cabecera = "HTTP/1.1 200 OK" + finLinea;

        //Obtiene el nombre del archivo solicitado y lo carga en un File
        String archivo = mensaje.split("/")[1].split(" ")[0];
        //Imprmimimos el nombre del archivo obtenido de la solicitud
	    System.out.println("\nNombre de Archivo Solicitado: "+archivo+"\n\n" );

        String extension;
        //Este trycatch sirve por si el cliente no pone ninguna extension entonces
        //nosotros le ponemos una default para que aparezca el NotFound.html
		try {
			extension = archivo.split("\\.")[1];
		} catch (Exception e) {
			extension = "html";
		}

        //Se crea un File con la ruta del nombre buscado, si no existe en la carpeta raiz entonces el tamaño va a ser 0
        File file = new File("./raiz/" + archivo);

        String fechaModificacion = "";

        //Si el tamaño del archivo es 0 entonces se cambia la cabecera a 404 not found
        if(file.length() == 0){
        	cabecera = "HTTP/1.1 404 Not Found" + finLinea;
        	File notfound = new File("./raiz/NotFound.html");
        	fechaModificacion = obtenerFechaUltimaModificacion(notfound);
        }else{
        	fechaModificacion = obtenerFechaUltimaModificacion(file);
        }

        //Se obtiene la palabra anterior a cada extension obtenida anteriormente
        String preExt = "text";
        //Verifica que tipo de extension es, si es png, ico o jpeg entonces es un image
        if (extension.equals("png") || extension.equals("ico") || extension.equals("jpeg")
        		|| extension.equals("jpg")) {
            preExt = "image";
        }
        //Si la extension es pdf entonces es un tipo document
        if (extension.equals("pdf")) {
            preExt = "doc";
        }

        //Se concatena a la cabecera las fechas
        cabecera += "Date: " + date + finLinea + "Content-Type: " + preExt + "/" + extension + finLinea;
        cabecera += "Last-Modified: " + fechaModificacion + finLinea;

      //Transforma el archivo solicitado en bytes, para poder enviarlo al cliente
        byte[] archivoToSend = archivoToBytes(file);
        int tamano = archivoToSend.length;
        cabecera += "Content-Length: " + tamano + finLinea + finLinea;
        //Imprmimimos el mensaje de respuesta del server
	    System.out.println("\n[Server]Mensaje de Respuesta From Server: \n"+cabecera+"\n\n" );
        //Crea el byteBuffer que llevara el mensaje para enviarRespuesta
        //al cliente, con la longitud necesaria para la cabecera y el contenido
        ByteBuffer respuesta = ByteBuffer.allocate(cabecera.getBytes().length +
        		archivoToSend.length + finLinea.getBytes().length);

        //Se concatenan los bytes del mensaje de respuesta, el contenido (archivo a mandar) y un salto de linea
        respuesta.put(cabecera.getBytes());
        respuesta.put(archivoToSend);
        respuesta.put(finLinea.getBytes());

        //Se retorna la respuesta en forma de array de bytes
        return respuesta.array();
    }

    /**
	 * Metodo para obtener la fecha en GMT
	 * @return
	 */
	private static String obtenerFecha() {
		Date localTime = new Date();
        DateFormat s = new SimpleDateFormat("dd/MM/yyyy"+ " "+ " HH:mm:ss");
        s.setTimeZone(TimeZone.getTimeZone("GMT"));
		return localTime+" GMT";
	}

	/**
	 * Metodo para obtener la ultima fecha de modificacion del archivo en GMT
	 * @param archivo
	 * @return
	 */
	private static String obtenerFechaUltimaModificacion(File archivo) {
		long milisec = archivo.lastModified();
		Date date = new Date();
		date.setTime(milisec);
        DateFormat s = new SimpleDateFormat("dd/MM/yyyy"+ " "+ " HH:mm:ss");
        s.setTimeZone(TimeZone.getTimeZone("GMT"));
		return s.format(date)+" GMT";
	}

	/**
	 * Metodo para obtener un nuevo objeto de tipo BufferedReader
	 * @param socket
	 * @return
	 */
    private static BufferedReader getBufferedReader(Socket socket) {
        BufferedReader fromNetwork = null;
        try {
            fromNetwork = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println(e);
        }
        return fromNetwork;
    }

    /**
     * Metodo para obtener un nuevo objeto OutputStream por donde se mandará el contenido por el socket
     * @param socket
     * @return
     */
    private static OutputStream getOutputStream(Socket socket) {
        OutputStream toNetwork = null;
        try {
            toNetwork = socket.getOutputStream();
        } catch (IOException ex) {
            System.out.println(ex);
        }
        return toNetwork;
    }

    /**
     * Metodo para recibir cada linea del mensaje de solicitud del BufferedReader
     * @param fromNetwork
     * @return
     */
    private static String recibirPeticion(BufferedReader fromNetwork) {
        String mensaje = null;
        try {
            mensaje = fromNetwork.readLine();
        } catch (IOException e) {
            System.out.println(e);
        }
        return mensaje;
    }

    /**
     * Metodo que convierte el archivo de tipo File a un array de Bytes
     * @param file
     * @return
     */
    private static byte[] archivoToBytes(File file) {
        byte[] archivo_bytes = null;
        try {
        	//Esto es una pequeña comprobacion para ver si el archivo que se busca existe
        	//Y si no existe entonces se mandan los bytes del archivo NotFound.html
        	if(file.length()>0){
        		archivo_bytes = Files.readAllBytes(file.toPath());
        	}else{
        		File notfound = new File("./raiz/NotFound.html");
        		archivo_bytes = Files.readAllBytes(notfound.toPath());
        	}
        } catch (IOException ex) {
            System.out.println(ex);
        }
        return archivo_bytes;
    }

    /**
     * Metodo para enviar la respuesta por el Socket por el OutputStream por medio del metodo write()
     * @param toNetwork
     * @param archivo_bytes
     */
    private static void enviarRespuesta(OutputStream toNetwork, byte[] respuesta) {
        try {
            toNetwork.write(respuesta);
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    /**
     * Metodo que cierra el BufferedReader y el OutputStream cuando se acaba la conexión
     * @param fromNetwork
     * @param toNetwork
     */
    private static void cerrarCanales(BufferedReader fromNetwork, OutputStream toNetwork) {
        try {
            fromNetwork.close();
            toNetwork.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

}
