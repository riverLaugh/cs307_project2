import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

/**
 * �ͻ���.
 *
 * @author jialin.li
 * @date 2019-12-10 22:30
 */
public class socketClient {
    public static void main(String[] args) throws IOException {
        // ָ��ip �˿ڣ�����socket
        String host = "127.0.0.1";
        int port = 8099;
        Socket communicationSocket = new Socket(host, port);
        // ��ȡ������,��ȡ������������Ϣ
        InputStream inputStream = communicationSocket.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        OutputStream outputStream = communicationSocket.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        String message;
        bufferedWriter.write("try\n");
        bufferedWriter.flush();
        message = bufferedReader.readLine();
        Scanner in = new Scanner(System.in);
        if (Objects.equals(message, "welcome")) {
            System.out.println("Welcome!You can input the command now!");
        }
        while (true) {
//            System.out.println(1);
            String cmd = in.nextLine();
            bufferedWriter.write(cmd+"\n");
            bufferedWriter.flush();
            message = bufferedReader.readLine();
            System.out.printf("%s", message);
        }
//            communicationSocket.shutdownInput();
//            // ��ȡ�����,д������
//            communicationSocket.shutdownOutput();
        // �ر���Դ
//        bufferedWriter.close();
//        bufferedReader.close();
    }
}