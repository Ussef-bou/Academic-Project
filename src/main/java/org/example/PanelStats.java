package org.example;

import com.toedter.calendar.JDateChooser;

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


    private JDateChooser dateDebutChooser;
    private JDateChooser dateFinChooser;
    private JComboBox<String> comboServeur;
    private JButton btnFiltrerStats;
    private JButton btnFiltrerCommandes;

    public PanelStats(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(10, 10));

        JTabbedPane tabs = new JTabbedPane();


        JPanel panelGlobal = new JPanel(new BorderLayout(5, 5));


        modelStats = new DefaultTableModel(new String[]{"Date", "Total Journée", "Nombre Tickets", "Nombre Clients"}, 0);
        tableStats = new JTable(modelStats);

        JPanel panelFiltreStats = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dateDebutChooser = new JDateChooser();
        dateDebutChooser.setDateFormatString("yyyy-MM-dd");
        dateFinChooser = new JDateChooser();
        dateFinChooser.setDateFormatString("yyyy-MM-dd");
        comboServeur = new JComboBox<>();
        comboServeur.addItem("Tous");
        chargerServeurs();
        btnFiltrerStats = new JButton("Filtrer Stats");

        panelFiltreStats.add(new JLabel("Date début:"));
        panelFiltreStats.add(dateDebutChooser);
        panelFiltreStats.add(new JLabel("Date fin:"));
        panelFiltreStats.add(dateFinChooser);
        panelFiltreStats.add(new JLabel("Serveur:"));
        panelFiltreStats.add(comboServeur);
        panelFiltreStats.add(btnFiltrerStats);

        panelGlobal.add(panelFiltreStats, BorderLayout.NORTH);
        panelGlobal.add(new JScrollPane(tableStats), BorderLayout.CENTER);

        btnFiltrerStats.addActionListener(e -> {
            String dateDebut = dateDebutChooser.getDate() != null ?
                    new java.sql.Date(dateDebutChooser.getDate().getTime()).toString() : null;
            String dateFin = dateFinChooser.getDate() != null ?
                    new java.sql.Date(dateFinChooser.getDate().getTime()).toString() : null;
            String serveur = (String) comboServeur.getSelectedItem();
            chargerStats(dateDebut, dateFin, serveur);
        });

        JPanel panelCommandes = new JPanel(new BorderLayout(5, 5));
        modelCommandes = new DefaultTableModel(
                new String[]{"Article", "Quantité", "Prix Unitaire", "Total", "Client", "Table", "Opérateur", "Date/Heure"}, 0);
        tableCommandes = new JTable(modelCommandes);

        JPanel panelFiltreCmd = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JDateChooser dateDebutCmd = new JDateChooser();
        dateDebutCmd.setDateFormatString("yyyy-MM-dd");
        JDateChooser dateFinCmd = new JDateChooser();
        dateFinCmd.setDateFormatString("yyyy-MM-dd");
        JComboBox<String> comboServeurCmd = new JComboBox<>();
        comboServeurCmd.addItem("Tous");
        chargerServeurs(comboServeurCmd);
        btnFiltrerCommandes = new JButton("Filtrer Commandes");

        panelFiltreCmd.add(new JLabel("Date début:"));
        panelFiltreCmd.add(dateDebutCmd);
        panelFiltreCmd.add(new JLabel("Date fin:"));
        panelFiltreCmd.add(dateFinCmd);
        panelFiltreCmd.add(new JLabel("Serveur:"));
        panelFiltreCmd.add(comboServeurCmd);
        panelFiltreCmd.add(btnFiltrerCommandes);

        panelCommandes.add(panelFiltreCmd, BorderLayout.NORTH);
        panelCommandes.add(new JScrollPane(tableCommandes), BorderLayout.CENTER);

        btnFiltrerCommandes.addActionListener(e -> {
            String dateDebut = dateDebutCmd.getDate() != null ?
                    new java.sql.Date(dateDebutCmd.getDate().getTime()).toString() : null;
            String dateFin = dateFinCmd.getDate() != null ?
                    new java.sql.Date(dateFinCmd.getDate().getTime()).toString() : null;
            String serveur = (String) comboServeurCmd.getSelectedItem();
            chargerCommandesPassees(dateDebut, dateFin, serveur);
        });

        tabs.addTab("Statistiques Globales", panelGlobal);
        tabs.addTab("Commandes Passées", panelCommandes);

        add(tabs, BorderLayout.CENTER);

        chargerStats(null, null, "Tous");
        chargerCommandesPassees(null, null, "Tous");
    }

    private void chargerStats(String dateDebut, String dateFin, String serveur) {
        modelStats.setRowCount(0);
        StringBuilder sql = new StringBuilder(
                "SELECT DATE(c.date_heure) AS date_vente, " +
                        "SUM(c.prix_total) AS total_jour, " +
                        "COUNT(DISTINCT c.ticket_id) AS nombre_tickets, " +
                        "COUNT(DISTINCT c.client_id) AS nombre_clients " +
                        "FROM commande c " +
                        "JOIN operateur o ON c.operateur_id = o.id " +
                        "WHERE 1=1"
        );

        if (dateDebut != null) sql.append(" AND DATE(c.date_heure) >= ?");
        if (dateFin != null) sql.append(" AND DATE(c.date_heure) <= ?");
        if (serveur != null && !serveur.equals("Tous")) sql.append(" AND o.nom_operateur = ?");
        sql.append(" GROUP BY DATE(c.date_heure) ORDER BY DATE(c.date_heure) DESC");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (dateDebut != null) ps.setString(idx++, dateDebut);
            if (dateFin != null) ps.setString(idx++, dateFin);
            if (serveur != null && !serveur.equals("Tous")) ps.setString(idx++, serveur);

            ResultSet rs = ps.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            while (rs.next()) {
                modelStats.addRow(new Object[]{
                        rs.getDate("date_vente") != null ? sdf.format(rs.getDate("date_vente")) : "",
                        rs.getBigDecimal("total_jour"),
                        rs.getInt("nombre_tickets"),
                        rs.getInt("nombre_clients")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement statistiques : " + e.getMessage());
        }
    }

    private void chargerCommandesPassees(String dateDebut, String dateFin, String serveur) {
        modelCommandes.setRowCount(0);
        StringBuilder sql = new StringBuilder(
                "SELECT a.nom_article, c.quantite, c.prix_unitaire, c.prix_total, cl.nom_client, " +
                        "t.nom_table, o.nom_operateur, c.date_heure " +
                        "FROM commande c " +
                        "JOIN article a ON c.article_id = a.id " +
                        "LEFT JOIN client cl ON c.client_id = cl.id " +
                        "LEFT JOIN les_tables t ON c.table_id = t.id " +
                        "JOIN operateur o ON c.operateur_id = o.id " +
                        "WHERE 1=1"
        );

        if (dateDebut != null) sql.append(" AND DATE(c.date_heure) >= ?");
        if (dateFin != null) sql.append(" AND DATE(c.date_heure) <= ?");
        if (serveur != null && !serveur.equals("Tous")) sql.append(" AND o.nom_operateur = ?");
        sql.append(" ORDER BY c.id DESC");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (dateDebut != null) ps.setString(idx++, dateDebut);
            if (dateFin != null) ps.setString(idx++, dateFin);
            if (serveur != null && !serveur.equals("Tous")) ps.setString(idx++, serveur);

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


    private void chargerServeurs() {
        chargerServeurs(this.comboServeur);
    }

    private void chargerServeurs(JComboBox<String> combo) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT nom_operateur FROM operateur")) {
            while (rs.next()) {
                combo.addItem(rs.getString("nom_operateur"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
