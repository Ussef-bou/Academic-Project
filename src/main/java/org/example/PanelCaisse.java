package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

public class PanelCaisse extends JPanel {

    private Connection conn;
    private int caisseId = -1;
    private JLabel lblOuverture, lblOperateur, lblCloture, lblTotalVentes, lblEtat;
    private DefaultTableModel modelHistorique;
    private JTable tableHistorique;
    private Timer timer;

    public PanelCaisse(Connection conn, int operateurId) {
        this.conn = conn;
        setLayout(new BorderLayout(10,10));

        // Panneau info
        JPanel infoPanel = new GridLayoutPanel();
        lblOuverture = new JLabel("Ouverture : -");
        lblOperateur = new JLabel("Opérateur : -");
        lblCloture = new JLabel("Clôture : -");
        lblTotalVentes = new JLabel("Total Ventes : -");
        lblEtat = new JLabel("Etat : -");

        infoPanel.add(lblOuverture);
        infoPanel.add(lblOperateur);
        infoPanel.add(lblCloture);
        infoPanel.add(lblTotalVentes);
        infoPanel.add(lblEtat);

        add(infoPanel, BorderLayout.NORTH);

        // Tableau historique
        String[] colonnes = {"ID", "Ouverture", "Opérateur", "Clôture", "Total Ventes"};
        modelHistorique = new DefaultTableModel(colonnes,0){
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableHistorique = new JTable(modelHistorique);
        add(new JScrollPane(tableHistorique), BorderLayout.CENTER);

        // Boutons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRafraichir = new JButton("Rafraîchir");
        JButton btnCloturer = new JButton("Clôturer Caisse");
        btnPanel.add(btnRafraichir);
        btnPanel.add(btnCloturer);
        add(btnPanel, BorderLayout.SOUTH);

        btnRafraichir.addActionListener(e -> rafraichir());
        btnCloturer.addActionListener(e -> cloturerCaisse(operateurId));

        // Ouvrir une caisse automatiquement si aucune n’est ouverte
        ouvrirCaisseSiAbsente(operateurId);

        // Rafraîchir affichage
        rafraichir();

        // Démarrer le timer pour mise à jour live toutes les 5s
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> rafraichirTotalVentesLive());
            }
        }, 0, 5000);
    }

    private void ouvrirCaisseSiAbsente(int operateurId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM caisse WHERE date_fermeture IS NULL AND operateur_id = ? LIMIT 1")) {
            ps.setInt(1, operateurId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                caisseId = rs.getInt("id");
            } else {
                try (PreparedStatement insert = conn.prepareStatement(
                        "INSERT INTO caisse (operateur_id, date_ouverture) VALUES (?, NOW())",
                        Statement.RETURN_GENERATED_KEYS)) {
                    insert.setInt(1, operateurId);
                    insert.executeUpdate();
                    ResultSet keys = insert.getGeneratedKeys();
                    if (keys.next()) caisseId = keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void rafraichir() {
        if (caisseId != -1) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT c.*, o.nom_operateur FROM caisse c " +
                            "LEFT JOIN operateur o ON c.operateur_id = o.id " +
                            "WHERE c.id = ?")) {
                ps.setInt(1, caisseId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Timestamp ouverture = rs.getTimestamp("date_ouverture");
                    Timestamp cloture = rs.getTimestamp("date_fermeture");

                    lblOuverture.setText("Ouverture : " + ouverture);
                    lblOperateur.setText("Opérateur : " + rs.getString("nom_operateur"));
                    lblCloture.setText(cloture != null ? "Clôture : " + cloture : "Clôture : -");

                    double totalVentes = getTotalVentes(ouverture, cloture);
                    lblTotalVentes.setText(String.format("Total Ventes : %.2f", totalVentes));

                    lblEtat.setText(cloture == null ? "Etat : Ouverte" : "Etat : Fermée");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        rafraichirHistorique();
    }

    private void rafraichirHistorique() {
        modelHistorique.setRowCount(0);
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT c.*, o.nom_operateur FROM caisse c " +
                            "LEFT JOIN operateur o ON c.operateur_id = o.id " +
                            "ORDER BY c.date_ouverture DESC"
            );
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            while (rs.next()) {
                Timestamp ouverture = rs.getTimestamp("date_ouverture");
                Timestamp cloture = rs.getTimestamp("date_fermeture");
                int id = rs.getInt("id");
                double totalVentes = getTotalVentes(ouverture, cloture);

                Object[] row = {
                        id,
                        sdf.format(ouverture),
                        rs.getString("nom_operateur"),
                        cloture != null ? sdf.format(cloture) : "-",
                        totalVentes
                };
                modelHistorique.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void rafraichirTotalVentesLive() {
        if (caisseId != -1) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT date_ouverture FROM caisse WHERE id = ?")) {
                ps.setInt(1, caisseId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Timestamp ouverture = rs.getTimestamp("date_ouverture");
                    double total = getTotalVentes(ouverture, null);
                    lblTotalVentes.setText(String.format("Total Ventes : %.2f", total));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private double getTotalVentes(Timestamp ouverture, Timestamp cloture) {
        double total = 0;
        if (ouverture == null) return 0;

        String sql = "SELECT SUM(prix_total) AS total " +
                "FROM commande " +
                "WHERE etatcmd='payée' AND date_heure BETWEEN ? AND ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, ouverture);
            ps.setTimestamp(2, cloture != null ? cloture : new Timestamp(System.currentTimeMillis()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) total = rs.getDouble("total");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }


    private void cloturerCaisse(int operateurId) {
        if (caisseId == -1) {
            JOptionPane.showMessageDialog(this, "Aucune caisse ouverte !");
            return;
        }

        // Vérifier que l'opérateur actuel est celui qui a ouvert la caisse
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT operateur_id FROM caisse WHERE id = ?")) {
            ps.setInt(1, caisseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt("operateur_id") != operateurId) {
                JOptionPane.showMessageDialog(this, "Seul l'opérateur ayant ouvert la caisse peut la clôturer !");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Vérification commandes non payées
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) AS nb FROM commande c " +
                        "JOIN ticket t ON c.ticket_id = t.id " +
                        "WHERE t.date_heure BETWEEN (SELECT date_ouverture FROM caisse WHERE id=?) AND NOW() " +
                        "AND c.etatcmd != 'payée'")) {
            ps.setInt(1, caisseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt("nb") > 0) {
                JOptionPane.showMessageDialog(this,
                        "Impossible de clôturer : il existe des commandes non payées !");
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        // Clôture
        Timestamp ouverture = getCaisseTimestamp("date_ouverture");
        double totalVentes = getTotalVentes(ouverture, new Timestamp(System.currentTimeMillis()));
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE caisse SET date_fermeture = NOW(), solde_final = ? WHERE id = ?")) {
            ps.setDouble(1, totalVentes);
            ps.setInt(2, caisseId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this,
                    String.format("Caisse clôturée avec succès ! Total ventes : %.2f", totalVentes));
            rafraichir();
            caisseId = -1;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Timestamp getCaisseTimestamp(String colonne) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT " + colonne + " FROM caisse WHERE id = ?")) {
            ps.setInt(1, caisseId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getTimestamp(colonne);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Helper panel pour GridLayout
    private static class GridLayoutPanel extends JPanel {
        GridLayoutPanel() {
            super(new GridLayout(2, 3, 10, 10));
        }
    }
}
