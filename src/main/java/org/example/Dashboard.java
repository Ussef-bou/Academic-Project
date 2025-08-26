package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;

public class Dashboard extends JFrame {

    private final Connection conn;
    private final int operateurId;
    private final JPanel contentPanel;
    private String nomGroupe;

    JButton btnCommandes = new JButton("Commandes");
    JButton btnParametres = new JButton("Paramètres");
    JButton btncloture=new JButton("Fin du tour");


    public Dashboard(Connection conn, int operateurId,String nomGroupe) {
        this.conn = conn;
        this.operateurId = operateurId;
        this.nomGroupe=nomGroupe;


        setTitle(" Système de Caisse");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());


        setIconImage(new ImageIcon(getClass().getResource("/coda.jpg")).getImage());

        contentPanel = new JPanel(new CardLayout());
        add(contentPanel, BorderLayout.CENTER);
        JPanel bottomMenu = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));






        btnCommandes.addActionListener(e -> switchPanel("commandes"));
        btnParametres.addActionListener(e -> switchPanel("parametres"));
        btncloture.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Voulez-vous vous déconnecter ?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {

                Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    window.dispose();
                }
                SwingUtilities.invokeLater(() -> {
                    Main.main(null);
                });
            }
        });

        bottomMenu.add(btnCommandes);
        bottomMenu.add(btnParametres);
        bottomMenu.add(btncloture);

        add(bottomMenu, BorderLayout.SOUTH);

        contentPanel.add(new PanelCommandes(conn, operateurId),"commandes");
        contentPanel.add(new PanelArticles(conn), "articles");
        contentPanel.add(new PanelClients(conn), "clients");
        contentPanel.add(new PanelTickets(conn), "tickets");
        contentPanel.add(new PanelCaisse(conn), "caisse");
        contentPanel.add(new PanelStats(conn), "statistiques");
        contentPanel.add(new PanelParametres(conn, this), "parametres");
        contentPanel.add(new Paneloperateur(conn), "operateur");

        switchPanel("commandes");
        setVisible(true);


restriction();

    }
    public void switchPanel(String name){
        CardLayout cl = (CardLayout) contentPanel.getLayout();
        cl.show(contentPanel, name);
    }

    public void restriction(){
        switch(nomGroupe){
            case "Admin":
                break;
            case "serveur":
                btnParametres.setEnabled(false);
                break;
            case"caissier":
                btnParametres.setEnabled(false);
                break;

        }
    }
}