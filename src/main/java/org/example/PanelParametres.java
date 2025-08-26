package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;



public class PanelParametres extends JPanel {

    public PanelParametres(Connection conn ,Dashboard dash) {
        setLayout(new GridLayout(4, 2, 10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton btnArticles = new JButton("Articles");
        JButton btnClients = new JButton("Clients");
        JButton btnTickets = new JButton("Tickets");
        JButton btnCaisse = new JButton("Caisse");
        JButton btnStats = new JButton("statistiques");
        JButton btnopp= new JButton("operateur");


        add(btnArticles);
        add(btnClients);
        add(btnTickets);
        add(btnCaisse);
        add(btnStats);
        add(btnopp);


        btnStats.addActionListener(e -> dash.switchPanel("statistiques"));
        btnCaisse.addActionListener(e -> dash.switchPanel("caisse"));
        btnArticles.addActionListener(e -> dash.switchPanel("articles"));
        btnClients.addActionListener(e -> dash.switchPanel("clients"));
        btnTickets.addActionListener(e -> dash.switchPanel("tickets"));
        btnopp.addActionListener(e -> dash.switchPanel("operateur"));





    }

}
