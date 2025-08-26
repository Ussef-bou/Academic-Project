package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class PanelStats extends JPanel {
    private Connection conn;

    private DefaultTableModel modelStats;
    private DefaultTableModel modelCommandes;
    private JTable tableStats;
    private JTable tableCommandes;

    public PanelStats(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(10, 10));

        JTabbedPane tabs = new JTabbedPane();


        JPanel panelGlobal = new JPanel(new BorderLayout(5, 5));
        modelStats = new DefaultTableModel(new String[]{"Date", "Total Journée", "Nombre Tickets", "Nombre Clients"}, 0);
        tableStats = new JTable(modelStats);
        panelGlobal.add(new JScrollPane(tableStats), BorderLayout.CENTER);
        JButton btnStats = new JButton("🔄 Rafraîchir Stats");
        btnStats.addActionListener(e -> chargerStats());
        panelGlobal.add(btnStats, BorderLayout.NORTH);


        JPanel panelCommandes = new JPanel(new BorderLayout(5, 5));
        modelCommandes = new DefaultTableModel(new String[]{"Article", "Quantité", "Prix Unitaire", "Total", "Client", "Table", "Opérateur", "Date/Heure"}, 0);
        tableCommandes = new JTable(modelCommandes);
        panelCommandes.add(new JScrollPane(tableCommandes), BorderLayout.CENTER);
        JButton btnCommandes = new JButton("🔄 Afficher Commandes");
        btnCommandes.addActionListener(e -> chargerCommandesPassees());
        panelCommandes.add(btnCommandes, BorderLayout.NORTH);


        tabs.addTab("Statistiques Globales", panelGlobal);
        tabs.addTab("Commandes Passées", panelCommandes);

        add(tabs, BorderLayout.CENTER);

        chargerStats();
    }

    private void chargerStats() {
        modelStats.setRowCount(0);
        String sql = "SELECT CURDATE() AS date_vente, " +
                "SUM(prix_total) AS total_jour, " +
                "COUNT(*) AS nombre_tickets, " +
                "COUNT(DISTINCT client_id) AS nombre_clients " +
                "FROM commande " +
                "WHERE DATE(date_heure) = CURDATE()";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            while (rs.next()) {
                Date date = rs.getDate("date_vente");
                modelStats.addRow(new Object[]{
                        date != null ? sdf.format(date) : "",
                        rs.getBigDecimal("total_jour"),
                        rs.getInt("nombre_tickets"),
                        rs.getInt("nombre_clients")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement statistiques : " + e.getMessage());
        }
    }

    private void chargerCommandesPassees() {
        modelCommandes.setRowCount(0);
        String sql = "SELECT a.nom_article, c.quantite, c.prix_unitaire, c.prix_total, cl.nom_client, " +
                "t.nom_table, o.nom_operateur, c.date_heure " +
                "FROM commande c " +
                "JOIN article a ON c.article_id = a.id " +
                "LEFT JOIN client cl ON c.client_id = cl.id " +
                "LEFT JOIN les_tables t ON c.table_id = t.id " +
                "JOIN operateur o ON c.operateur_id = o.id " +
                "ORDER BY c.id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modelCommandes.addRow(new Object[]{
                        rs.getString("nom_article"),
                        rs.getInt("quantite"),
                        rs.getDouble("prix_unitaire"),
                        rs.getDouble("prix_total"),
                        rs.getString("nom_client"),
                        rs.getString("nom_table"),
                        rs.getString("nom_operateur"),
                        rs.getString("date_heure")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement commandes : " + e.getMessage());
        }
    }
}
