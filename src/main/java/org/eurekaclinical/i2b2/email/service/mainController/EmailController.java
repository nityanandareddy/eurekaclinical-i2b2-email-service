package org.eurekaclinical.i2b2.email.service.mainController;

/*-
 * #%L
 * eurekaclinical-i2b2-email-service
 * %%
 * Copyright (C) 2016 - 2019 Emory University
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;

public class EmailController {

    private Properties props;
    private static Map<String,String> propMap;
    public static void main(String[] args) throws Exception{

        System.out.println("In email controller");

        EmailController controller = new EmailController();
        controller.loadApplicationProperties();
        Connection con = controller.getConnection(propMap.get("driverClassName"),propMap.get("dbuser"),propMap.get("dbpassword"),propMap.get("dbUrl"));
        List<String> emailList = controller.getEmailIds(con);
        String body = controller.getEmailBody(emailList,propMap.get("listServListName"),propMap.get("senderPassword"));
        controller.mailtoListServ(body,propMap.get("serverName"),propMap.get("senderEmailId"),propMap.get("recipientEmailId"));
    }
    private void loadApplicationProperties() throws Exception
    {
        String[] propNames = {"dbUrl","driverClassName","dbuser","dbpassword","listServListName","senderEmailId","recipientEmailId","senderPassword","serverName"};
        propMap = new HashMap<>();
        props = new Properties();
        InputStream input = null;

        try {

            String propFile = "application.properties";
            input = EmailController.class.getClassLoader().getResourceAsStream(propFile);
            if(input==null){
                System.out.println("Sorry, unable to find " + propFile);
                throw  new FileNotFoundException();
            }

            //load a properties file from class path, inside static method
            props.load(input);

            for (String prop: propNames) {
                propMap.put(prop,props.getProperty(prop));
            }

            if(propMap.containsValue(null) || propMap.containsValue(""))
            {
                System.out.println("Following properties not defined in the properties file. Please enter the properties in application.properties file."+propNames);
                throw new Exception();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally{
            if(input!=null){
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Connection getConnection(String driverClassName,String dbuser,String dbpassword,String dbUrl)
    {
        Connection conn =null;
        try {

            Class.forName(driverClassName);
            Properties props = new Properties();
            props.setProperty("user", dbuser);
            props.setProperty("password", dbpassword);
            props.setProperty("ssl", "true");
            conn = DriverManager.getConnection(dbUrl, props);

        }catch (Exception e)
        {
            System.out.println("Please include correct library for database and give correct URL, credentials in properties file.");
            e.printStackTrace();
        }

        return conn;
    }
    private List<String> getEmailIds(Connection conn) throws  SQLException
    {
        try {

            List<String> emailList = new ArrayList<String>();
            String getEmailsQueryStr = "SELECT EMAIL FROM PM.USERS;";
            Statement stmt = conn.createStatement();
            ResultSet rset = stmt.executeQuery(getEmailsQueryStr);
            int count = 0;
            while (rset.next()) {
                rset.getString("email");
                count++;
            }
            System.out.println("EmailIds Count:" + count);
            return emailList;
        }
        finally {
            conn.close();
        }
    }
    private String getEmailBody(List<String> emailList, String listServListName, String senderPassword)
    {
        String addCommand = "[QUIT] ADD "+propMap.get(listServListName)+" DD=job1 PW="+propMap.get(senderPassword) +"\n\n"+"//job1 DD#\n";
        StringBuilder emailsString = new StringBuilder();
        emailsString.append(addCommand);
        for (String emailId: emailList) {
            emailsString.append("\n").append(emailId);
        }
        emailsString.append("\n").append("#/");
        return emailsString.toString();
    }

    private void mailtoListServ(String body, String serverName, String senderEmailId,String recipientEmailId) throws MessagingException
    {
        Properties properties = System.getProperties();

        // Setting up mail server
        properties.setProperty("mail.smtp.host", propMap.get(serverName));

        // creating session object to get properties
        Session session = Session.getDefaultInstance(properties);

        MimeMessage message = new MimeMessage(session);

        // Set From Field: adding senders email to from field.
        message.setFrom(new InternetAddress(propMap.get(senderEmailId)));

        // Set To Field: adding recipient's email to from field.
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(propMap.get(recipientEmailId)));

        // Set Subject: subject of the email
        message.setSubject("");

        // set body of the email.
        message.setText(body);

        // Send email.
        Transport.send(message);
        System.out.println("Mail successfully sent");
    }
}
