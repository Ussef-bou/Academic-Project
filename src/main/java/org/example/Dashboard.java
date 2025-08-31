package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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




        JButton operateur=new JButton("Operateur : %s".formatted(nomop(operateurId)));

        btnCommandes.addActionListener(e -> switchPanel("commandes"));
        btnParametres.addActionListener(e -> switchPanel("parametres"));
        btncloture.addActionListener(e -> {
            try {

                String sql = "SELECT COUNT(*) FROM commande WHERE operateur_id=? AND etatcmd='validée'";
                boolean commandesNonPayees = false;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, operateurId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next() && rs.getInt(1) > 0) {
                        commandesNonPayees = true;
                    }
                }

                if (commandesNonPayees) {
                    JOptionPane.showMessageDialog(this,
                            "Il reste des commandes non payées. Veuillez les clôturer avant de vous déconnecter.",
                            "Attention",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(this,
                        "Voulez-vous vous déconnecter ?",
                        "Confirmation",
                        JOptionPane.YES_NO_OPTION);
                if (confirm != JOptionPane.YES_OPTION) return;
                Window window = SwingUtilities.getWindowAncestor((Component) e.getSource());
                if (window != null) window.dispose();

                if (conn != null && !conn.isClosed()) conn.close();
                SwingUtilities.invokeLater(() -> Main.main(null));

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Erreur lors de la vérification des commandes : " + ex.getMessage(),
                        "Erreur SQL",
                        JOptionPane.ERROR_MESSAGE);
            }
        });


        bottomMenu.add(operateur);
        bottomMenu.add(btnCommandes);
        bottomMenu.add(btnParametres);
        bottomMenu.add(btncloture);

        add(bottomMenu, BorderLayout.SOUTH);

        contentPanel.add(new PanelCommandes(conn, operateurId,nomGroupe),"commandes");
        contentPanel.add(new PanelArticles(conn), "articles");
        contentPanel.add(new PanelClients(conn), "clients");
        contentPanel.add(new PanelTickets(conn), "tickets");
        contentPanel.add(new PanelCaisse(conn,operateurId), "caisse");
        contentPanel.add(new PanelStats(conn), "statistiques");
        contentPanel.add(new PanelParametres(conn, this), "parametres");
        contentPanel.add(new Paneloperateur(conn), "operateur");

        switchPanel("commandes");
        setVisible(true);


restriction();

    }
    private String nomop(int operateur_id){
        String sql="SELECT nom_operateur from operateur where id=?";
        try(PreparedStatement ps=conn.prepareStatement(sql)){
            ps.setInt(1,operateur_id);

            ResultSet r=ps.executeQuery();
            if(r.next()){
                return r.getString("nom_operateur");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return "Inconnu";
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
                btnParametres.setVisible(false);
                break;
            case"caissier":
                btnParametres.setEnabled(false);
                btnParametres.setVisible(false);


                break;

        }
    }
}