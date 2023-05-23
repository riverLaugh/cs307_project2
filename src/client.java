import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class client {
    public static void main(String[] args) {
        try{
        String host = "127.0.0.1";
        int port = 8919;
        Socket client = new Socket(host, port);
        Scanner in = new Scanner(System.in);
        while(true){




            String cmd = in.nextLine();
            Writer writer = new OutputStreamWriter(client.getOutputStream());
            writer.write(cmd);
            writer.flush();
        }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
