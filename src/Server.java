import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable{

    private ArrayList<ConnectionHandler> connections;
    private ServerSocket server;
    private boolean done;
    private ExecutorService pool;


    public Server() {
        connections = new ArrayList<>();
        done = false;
    }


    /**
     * create a thread, pass the whole class so that it's runnable
     * server will constantly listen for incoming connection requests
     * will open a new connection handler for each new client connection
     */
    @Override
    public void run(){

        try {
            server = new ServerSocket(9999);
            pool = Executors.newCachedThreadPool();

            while (!done) {
                Socket client = server.accept();
                ConnectionHandler handler = new ConnectionHandler(client);
                connections.add(handler);
                pool.execute(handler);
            }
        } catch (Exception e) {
            shutdown();
        }
    }

    public void broadcast(String message) {
        for (ConnectionHandler ch : connections) {
            if (ch != null) {
                ch.sendMessage(message);
            }
        }
    }

    public void shutdown() {

        try {
            done = true;
            if (!server.isClosed()) {
                server.close();
            }

            for (ConnectionHandler ch : connections) {
                ch.shutdown();
            }
        } catch (IOException e) {
            //ignore
        }
    }

    class ConnectionHandler implements Runnable{

        private Socket client;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ConnectionHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            try{
                out = new PrintWriter(client.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(client.getInputStream()));

//                out.println("Hello");   Send something to the client
//                in.readLine();         Get a message from the client

                out.println("Please enter a nickname: ");
                nickname = in.readLine();

//                if (nickname.isBlank() || nickname.isEmpty() || nickname.length() < 8 || !nickname.equals("^[a-zA-Z0-9]*$")){
//                    out.println("Invalid nickname. Please enter a value longer than 8 characters  containing only alphanumeric characters: ");
//                    nickname = in.readLine();
//                }

                System.out.println(nickname + "connected!");
                broadcast(nickname + "joined the chat!");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/nick")) {
                        String[] messageSplit = message.split(" ", 2);
                        if (messageSplit.length == 2) {
                            broadcast(nickname + " renamed themselves to "+ messageSplit[1]);
                            System.out.println(nickname + " renamed themselves to "+ messageSplit[1]);
                            nickname = messageSplit[1];
                            out.println("Nickname changed successfully changed to: " + nickname);
                        } else {
                            out.println("No nickname provided");
                        }
                    } else if (message.startsWith("/quit")) {
                        broadcast(nickname + " left the chat.");
                        shutdown();
                    }else {
                        broadcast(nickname + ": " + message);
                    }
                }
            } catch (IOException e) {
                shutdown();
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }

        public void shutdown() {

            try {
                in.close();
                out.close();
                if (!client.isClosed()) {
                    client.close();
                }
            } catch (IOException e) {
                //ignore
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}
