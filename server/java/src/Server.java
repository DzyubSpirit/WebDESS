import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class Server {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create();
        server.bind(new InetSocketAddress(8765), 0);

        HttpContext context = server.createContext("/simulator", new SimulatorHandler());

        server.setExecutor(null);
        System.out.println("Server has been started on :8080");
        server.start();
    }

    static class SimulatorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            boolean isError = true;
            byte[] bytes = null;
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            try {
                long startTime = System.currentTimeMillis();
                bytes = LocalNetSimulator.run(new String(exchange.getRequestBody().readAllBytes())).getBytes();
                System.out.printf("Elapsed time: %d", System.currentTimeMillis()-startTime);
                isError = false;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (!isError) {
                    exchange.sendResponseHeaders(200, bytes.length);
                } else {
                    bytes = "Error".getBytes();
                    exchange.sendResponseHeaders(500, bytes.length);
                }

                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            }
        }
    }
}
