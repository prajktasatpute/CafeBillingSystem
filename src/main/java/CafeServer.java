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


    private static final String OWNER_EMAIL = "sayalisatpute22@gmail.com";
    private static final String OWNER_PASS = "bcxjrloylxdljnsw"; // sequrity reason i put dummy password
    // Put your 16-digit Gmail App Password here
    private static final String KITCHEN_EMAIL = "satputesayali20.01.2004@gmail.com";

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/order", new MyOrderHandler());
        server.setExecutor(null);

        System.out.println("Server started successfully at http://localhost:8080");
        server.start();
    }

    static class MyOrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {

                InputStream inputStream = exchange.getRequestBody();
                String formData = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);


                String customerName = "";
                String customerEmail = "";
                String tableNo = "";
                String orderedItemsText = "";
                double totalBill = 0.0;


                String[] pairs = formData.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length > 1) {
                        String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                        String val = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);

                        if (key.equals("name")) customerName = val;
                        if (key.equals("email")) customerEmail = val;
                        if (key.equals("tableNo")) tableNo = val;


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

                sendEmailToKitchen(customerName, tableNo, orderedItemsText, totalBill);

                sendEmailToCustomer(customerEmail, customerName, tableNo, orderedItemsText, totalBill);


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


    public static void sendEmailToCustomer(String recipientEmail, String customerName, String tableNo, String items, double total) {
        String subject = "Your Cafe Bill & Payment QR Code";

        // Dummy QR code API link generating a QR code image
        //String qrImageUrl = "https://api.qrserver.com/v1/create-qr-code/?size=180x180&data=Pay-Rs-" + total + "-For-Table-" + tableNo;

        String htmlBody = "<html><body style='font-family: Arial;'>"
                + "<h2>Hello " + customerName + ",</h2>"
                + "<p>Thank you for visiting Campus Cafe! Here is your bill summary:</p>"
                + "<hr>"
                + "<p><b>Table Number:</b> " + tableNo + "</p>"
                + "<p><b>Ordered Items:</b><br><pre>" + items + "</pre></p>"
                + "<h3 style='color: red;'>Total Bill: Rs. " + total + "</h3>"
                + "<br>"
               // + "<p><b>Scan below QR code to pay your bill:</b></p>"
              //  + "<img src='" + qrImageUrl + "' alt='Payment QR Code'>"
                + "<br><p>Regards,<br><b>Campus Cafe Team</b></p>"
                + "</body></html>";

        sendMailUtil(recipientEmail, subject, htmlBody, true);
    }


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