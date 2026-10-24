import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class CafeServer {

    // Fetch credentials securely from environment variables (Safe for GitHub & Cloud)
    private static final String OWNER_EMAIL = System.getenv("CAFE_EMAIL");
    private static final String OWNER_PASS = System.getenv("CAFE_PASSWORD");
    private static final String KITCHEN_EMAIL = "satputesayali20.01.2004@gmail.com";

    public static void main(String[] args) throws IOException {
        // Cloud platforms (like Render) assign a dynamic PORT. Fallback to 8080 for local testing.
        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isEmpty()) {
            port = Integer.parseInt(portEnv);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/order", new MyOrderHandler());
        server.setExecutor(null);

        System.out.println("Server started successfully on port " + port);
        server.start();
    }

    static class MyOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {

            // Enable CORS headers so your Vercel frontend can talk to this backend
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {

                // Read data coming from HTML form
                InputStream inputStream = exchange.getRequestBody();
                String formData = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                String customerName = "";
                String customerEmail = "";
                String tableNo = "";
                String orderedItemsText = "";
                double totalBill = 0.0;

                // Split form parameters
                String[] pairs = formData.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length > 1) {
                        String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                        String val = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);

                        if (key.equals("name")) customerName = val;
                        if (key.equals("email")) customerEmail = val;
                        if (key.equals("tableNo")) tableNo = val;

                        // If user selected food items
                        if (key.equals("food")) {
                            String[] itemParts = val.split("-");
                            String itemName = itemParts[0].trim();
                            double price = Double.parseDouble(itemParts[1].trim());

                            orderedItemsText += "- " + itemName + " (Rs. " + price + ")\n";
                            totalBill += price;
                        }
                    }
                }

                System.out.println("New order received from: " + customerName + " for Table " + tableNo);

                // 1. Send Email to Kitchen Counter
                sendEmailToKitchen(customerName, tableNo, orderedItemsText, totalBill);

                // 2. Send Email to Customer with Bill and QR Code image
                sendEmailToCustomer(customerEmail, customerName, tableNo, orderedItemsText, totalBill);

                // Response page shown in browser after ordering
                String responseHtml = "<html><body style='text-align:center; font-family:Arial; margin-top:50px;'>"
                        + "<h1 style='color:green;'>Order Placed Successfully!</h1>"
                        + "<p>Thank you <b>" + customerName + "</b>. Your order for <b>Table " + tableNo + "</b> has been sent.</p>"
                        + "<p>Total Amount: <b>Rs. " + totalBill + "</b></p>"
                        + "<p>Check your email for the itemized bill and payment QR code.</p>"
                        + "<br><a href='javascript:history.back()'>Back to Menu</a>"
                        + "</body></html>";

                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, responseHtml.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(responseHtml.getBytes());
                os.close();
            }
        }
    }

    // Method to send email to Kitchen
    public static void sendEmailToKitchen(String customerName, String tableNo, String items, double total) {
        String subject = "New Order - Table No: " + tableNo;
        String messageBody = "Hello Kitchen Team,\n\n"
                + "New order received from customer: " + customerName + "\n"
                + "Table Number: " + tableNo + "\n\n"
                + "Items Ordered:\n" + items + "\n"
                + "Total Amount: Rs. " + total + "\n\n"
                + "Please start preparation!";

        sendMailUtil(KITCHEN_EMAIL, subject, messageBody, false);
    }

    // Method to send email to Customer with QR Code image
    public static void sendEmailToCustomer(String recipientEmail, String customerName, String tableNo, String items, double total) {
        String subject = "Your Cafe Bill ";

        // Replace with your direct image link from Postimages or Imgur
      //  String qrImageUrl = "PASTE_YOUR_DIRECT_IMAGE_LINK_HERE.png";

        String htmlBody = "<html><body style='font-family: Arial;'>"
                + "<h2>Hello " + customerName + ",</h2>"
                + "<p>Thank you for visiting Campus Cafe! Here is your bill summary:</p>"
                + "<hr>"
                + "<p><b>Table Number:</b> " + tableNo + "</p>"
                + "<p><b>Ordered Items:</b><br><pre>" + items + "</pre></p>"
                + "<h3 style='color: red;'>Total Bill: Rs. " + total + "</h3>"
                + "<br>"
              //  + "<p><b>Scan below QR code to pay your bill:</b></p>"
             //   + "<img src='" + qrImageUrl + "' alt='Cafe Payment QR Code' width='200' height='200'>"
                + "<br><p>Regards,<br><b>Campus Cafe Team</b></p>"
                + "</body></html>";

        sendMailUtil(recipientEmail, subject, htmlBody, true);
    }

    // Common helper method to send emails using JavaMail
    public static void sendMailUtil(String toEmail, String subject, String content, boolean isHtml) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(OWNER_EMAIL, OWNER_PASS);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(OWNER_EMAIL, "Campus Cafe"));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            msg.setSubject(subject);

            if (isHtml) {
                msg.setContent(content, "text/html");
            } else {
                msg.setText(content);
            }

            Transport.send(msg);
            System.out.println("Email sent successfully to: " + toEmail);

        } catch (Exception e) {
            System.out.println("Failed to send email: " + e.getMessage());
        }
    }
}