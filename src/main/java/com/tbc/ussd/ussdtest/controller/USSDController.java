/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tbc.ussd.ussdtest.controller;

import com.google.gson.Gson;
import com.tbc.ussd.ussdtest.model.ClientState;
import com.tbc.ussd.ussdtest.model.UssdRequest;
import com.tbc.ussd.ussdtest.model.UssdResponse;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Theodore Elikem Attigah
 */
@RestController
public class USSDController {

    @RequestMapping(value = "/hello", method = RequestMethod.POST)
    @ResponseBody
    public void hello(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        // Set the output printer
        PrintWriter out = response.getWriter();

        try {
            // Variable holding the ussd response
            UssdResponse ussdResponse = new UssdResponse();

            // Receive the Ussd request raw data
            StringBuilder requestBuffer = new StringBuilder();
            String lineRead;
            while ((lineRead = request.getReader().readLine()) != null) {
                requestBuffer.append(lineRead);
            }

            // Parse the request raw data into the UssdRequest Object for better manipulation
            // since the request raw data comes in a json format.
            Gson gson = new Gson();
            UssdRequest ussdRequest = gson.fromJson(requestBuffer.toString(), UssdRequest.class);

            ClientState clientState = null;

            // Process the Ussd request
            if (ussdRequest != null) {
                // check the various request type
                if (ussdRequest.getType().equals("Initiation")) {
                    // Set the ussd response
                    ussdResponse.setType("Response");
                    ussdResponse.setMessage("Welcome to mBank.\n1. Deposit\n2. Withdraw\n3. Check Balance\n0. Cancel");

                } else if (ussdRequest.getType().equals("Response")) {
                    // response case studies
                    if (!ussdRequest.getMessage().isEmpty()) {

                        // check whether the lcient state is sent
                        if (ussdRequest.getClientState() == null) {
                            clientState = new ClientState();

                            // Free food is cussdRequesthosen
                            if (ussdRequest.getMessage().equals("1")) {
                                ussdResponse.setMessage("Select Bank \n1. GCB Bank\n2. Ecobank\n3. Stanbic Bank");
                                ussdResponse.setType("Response");
                                clientState.setCurrentState("Bank");
                                clientState.setTransactionType("Deposit");
                            } // Free drink is chosen
                            else if (ussdRequest.getMessage().equals("2")) {
                                ussdResponse.setMessage("Select Bank \n1. Bank 1\n2. Bank 2\n3. Bank 3");
                                ussdResponse.setType("Response");
                                clientState.setCurrentState("Bank");
                                clientState.setTransactionType("Withdrawal");
                            } // Free airtime is chosen
                            else if (ussdRequest.getMessage().equals("3")) {
                                ussdResponse.setMessage("Merchant PIN");
                                ussdResponse.setType("Response");
                                clientState.setCurrentState("PIN");
                                clientState.setTransactionType("Balance");
                            } else {
                                ussdResponse.setMessage("Invalid Option");
                                ussdResponse.setType("Release");
                            }
                        } else {// The client state is resent back to us

                            clientState = gson.fromJson(ussdRequest.getClientState(), ClientState.class);

                            if (clientState.getCurrentState().equals("Bank")) {
                                switch (ussdRequest.getMessage()) {
                                    case "1":
                                    case "2":
                                    case "3":
                                        ussdResponse.setMessage("Account Number\n");
                                        ussdResponse.setType("Response");
                                        clientState.setCurrentState("FAC");
                                        clientState.setBank(ussdRequest.getMessage());
                                        break;
                                    default:
                                        ussdResponse.setMessage("Unexpected Response.");
                                        ussdResponse.setType("Release");
                                        break;
                                }
                            } else if (clientState.getCurrentState().equals("FAC")) {
                                ussdResponse.setMessage("Confirm Account Number\n");
                                ussdResponse.setType("Response");
                                clientState.setCurrentState("SAC");
                                clientState.setAccountNumer(ussdRequest.getMessage());
                            } else if (clientState.getCurrentState().equals("SAC")) {
                                clientState.setConfirmAccountNumber(ussdRequest.getMessage());
                                //Check if first and second account number are the same
                                if (clientState.getAccountNumer().equals(clientState.getConfirmAccountNumber())) {
                                    ussdResponse.setMessage("Amount\n");
                                    ussdResponse.setType("Response");
                                    clientState.setCurrentState("AMT");
                                } else {
                                    ussdResponse.setMessage("Account Number mismatch\nAccount Number\n");
                                    ussdResponse.setType("Response");
                                    clientState.setCurrentState("FAC");
                                }

                            } else if (clientState.getCurrentState().equals("AMT")) {
                                clientState.setAmount(ussdRequest.getMessage());
                                clientState.setCurrentState("PIN");

                                ussdResponse.setMessage("Merchant PIN");
                                ussdResponse.setType("Response");
                            } else if (clientState.getCurrentState().equals("PIN")) {
                                clientState.setMerchantPin(ussdRequest.getMessage());
                                clientState.setCurrentState("END");

                                if (clientState.getTransactionType().equals("Deposit")) {
                                    ussdResponse.setMessage("Transaction is being processed");
                                }
                                if (clientState.getTransactionType().equals("Withdrawal")) {
                                    ussdResponse.setMessage("Waiting for authorization...");
                                }
                                if (clientState.getTransactionType().equals("Balance")) {
                                    ussdResponse.setMessage("Account Balance: ");
                                }

                                ussdResponse.setType("Release");

                            } else {
                                ussdResponse.setMessage("Unexpected Response.");
                                ussdResponse.setType("Release");
                            }

                        }
                    } else {
                        ussdResponse.setMessage("Invalid option.");
                        ussdResponse.setType("Release");
                    }
                }
            } else {
                ussdResponse.setMessage("Duh.");
                ussdResponse.setType("Release");
            }

            //Stringify clientState
            String clientStateString = null;
            if (clientState != null) {
                clientStateString = gson.toJson(clientState);
            }

            ussdResponse.setClientState(clientStateString);

            String responseJson = gson.toJson(ussdResponse);
            out.print(responseJson);
        } catch (Exception e) {
            // Send at least a message to the user in case of errors
            try {
                Gson gson = new Gson();
                UssdResponse ussdResponse = new UssdResponse();
                ussdResponse.setMessage("Invalid request");
                ussdResponse.setType("Release");
                String responseJson = gson.toJson(ussdResponse);
                out.print(responseJson);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        }

    }

}
