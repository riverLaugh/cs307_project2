import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;

/**
 * 服务器.
 *
 * @author jialin.li
 * @date 2019-12-10 19:45
 */
public class socketServer {
    public static void main(String[] args) throws IOException {
        int port = 8099;
        ServerSocket connectionSocket = new ServerSocket(port);
        // 监听端口，accept为阻塞方法
        System.out.println("Get socket successfully, wait for request...");
        Socket communicationSocket = connectionSocket.accept();
        // 获取输入流，读取数据
        InputStream inputStream = communicationSocket.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        OutputStream outputStream = communicationSocket.getOutputStream();
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        String message;
        message = bufferedReader.readLine();
        if(Objects.equals(message, "try")){
            bufferedWriter.write("welcome\n");
            bufferedWriter.flush();
        }
        while(true){
            message = bufferedReader.readLine();
            System.out.println(message);
            String respond = "the server have received the command:"+message+"\n";
            bufferedWriter.write(respond);
            bufferedWriter.flush();
        }

//        while ((message = bufferedReader.readLine()) != null) {
//            System.out.printf("get message from client : %s",message);
//        }
//        communicationSocket.shutdownInput();
//        // 获取输出流,返回结果
//        bufferedWriter.write("I got your message and the communication is over.");
//        bufferedWriter.flush();
//        communicationSocket.shutdownOutput();
//        // 关闭资源
//        bufferedWriter.close();
//        bufferedReader.close();
    }
}