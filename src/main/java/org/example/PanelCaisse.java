package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class PanelCaisse extends JPanel {

    private Connection conn;
    private int caisseId = -1;
    private JLabel lblOuverture, lblSoldeInitial, lblSoldeFinal;
    private DefaultTableModel modelMouvements;
    private JTable tableMouvements;

    public PanelCaisse(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(10,10));

        JPanel infoPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        lblOuverture = new JLabel("Ouverture : -");
        lblSoldeInitial = new JLabel("Solde Initial : -");
        lblSoldeFinal = new JLabel("Solde Final : -");
        infoPanel.add(lblOuverture);
        infoPanel.add(lblSoldeInitial);
        infoPanel.add(lblSoldeFinal);
        add(infoPanel, BorderLayout.NORTH);

        String[] colonnes = {"Type", "Motif", "Montant", "Date"};
        modelMouvements = new DefaultTableModel(colonnes,0){
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        tableMouvements = new JTable(modelMouvements);
        add(new JScrollPane(tableMouvements), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnOuvrir = new JButton("Ouvrir Caisse");
        JButton btnFermer = new JButton("Fermer Caisse");
        JButton btnAjouterMvt = new JButton("Ajouter Mouvement");
        JButton btnRafraichir = new JButton("Rafraîchir");
        btnPanel.add(btnOuvrir);
        btnPanel.add(btnFermer);
        btnPanel.add(btnAjouterMvt);
        btnPanel.add(btnRafraichir);
        add(btnPanel, BorderLayout.SOUTH);

        btnOuvrir.addActionListener(e -> ouvrirCaisse());
        btnFermer.addActionListener(e -> fermerCaisse());
        btnAjouterMvt.addActionListener(e -> ajouterMouvement());
        btnRafraichir.addActionListener(e -> rafraichir());

        rafraichir();
    }

    private void rafraichir() {

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, date_ouverture, solde_initial, solde_final FROM caisse WHERE date_fermeture IS NULL ORDER BY date_ouverture DESC LIMIT 1")) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                caisseId = rs.getInt("id");
                lblOuverture.setText("Ouverture : " + rs.getTimestamp("date_ouverture"));
                lblSoldeInitial.setText("Solde Initial : " + rs.getBigDecimal("solde_initial"));
                lblSoldeFinal.setText("Solde Final : " + rs.getBigDecimal("solde_final"));
                chargerMouvements();
            } else {
                caisseId = -1;
                lblOuverture.setText("Ouverture : -");
                lblSoldeInitial.setText("Solde Initial : -");
                lblSoldeFinal.setText("Solde Final : -");
                modelMouvements.setRowCount(0);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement caisse : " + e.getMessage());
        }
    }

    private void chargerMouvements() {
        modelMouvements.setRowCount(0);
        if (caisseId == -1) return;
        String sql = "SELECT type, motif, montant, date_mouvement FROM mouvement_caisse WHERE caisse_id = ? ORDER BY date_mouvement DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, caisseId);
            ResultSet rs = ps.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            while (rs.next()) {
                Object[] row = {
                        rs.getString("type"),
                        rs.getString("motif"),
                        rs.getBigDecimal("montant"),
                        sdf.format(rs.getTimestamp("date_mouvement"))
                };
                modelMouvements.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement mouvements : " + e.getMessage());
        }
    }

    private void ouvrirCaisse() {
        if (caisseId != -1) {
            JOptionPane.showMessageDialog(this, "La caisse est déjà ouverte !");
            return;
        }
        String soldeStr = JOptionPane.showInputDialog(this, "Solde initial :", "0.00");
        if (soldeStr == null) return; // annulation
        try {
            double soldeInitial = Double.parseDouble(soldeStr);
            String sql = "INSERT INTO caisse (date_ouverture, solde_initial, operateur_id) VALUES (NOW(), ?, NULL)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, soldeInitial);
                ps.executeUpdate();
            }
            rafraichir();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Solde initial invalide !");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur ouverture caisse : " + e.getMessage());
        }
    }

    private void fermerCaisse() {
        if (caisseId == -1) {
            JOptionPane.showMessageDialog(this, "Aucune caisse ouverte !");
            return;
        }
        String soldeStr = JOptionPane.showInputDialog(this, "Solde final :", "0.00");
        if (soldeStr == null) return;
        try {
            double soldeFinal = Double.parseDouble(soldeStr);
            String sql = "UPDATE caisse SET date_fermeture = NOW(), solde_final = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, soldeFinal);
                ps.setInt(2, caisseId);
                ps.executeUpdate();
            }
            rafraichir();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Solde final invalide !");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur fermeture caisse : " + e.getMessage());
        }
    }

    private void ajouterMouvement() {
        if (caisseId == -1) {
            JOptionPane.showMessageDialog(this, "Ouvrez la caisse avant d'ajouter un mouvement.");
            return;
        }
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Ajouter Mouvement", true);
        dialog.setLayout(new GridLayout(4,2,10,10));
        dialog.setSize(350, 200);
        dialog.setLocationRelativeTo(this);

        JComboBox<String> comboType = new JComboBox<>(new String[]{"ENTREE", "SORTIE"});
        JTextField tfMotif = new JTextField();
        JTextField tfMontant = new JTextField();

        dialog.add(new JLabel("Type :"));
        dialog.add(comboType);
        dialog.add(new JLabel("Motif :"));
        dialog.add(tfMotif);
        dialog.add(new JLabel("Montant :"));
        dialog.add(tfMontant);

        JButton btnEnregistrer = new JButton("Enregistrer");
        JButton btnAnnuler = new JButton("Annuler");
        dialog.add(btnEnregistrer);
        dialog.add(btnAnnuler);

        btnEnregistrer.addActionListener(e -> {
            String type = (String) comboType.getSelectedItem();
            String motif = tfMotif.getText().trim();
            String montantStr = tfMontant.getText().trim();
            if (motif.isEmpty() || montantStr.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Remplissez tous les champs.");
                return;
            }
            double montant;
            try {
                montant = Double.parseDouble(montantStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Montant invalide.");
                return;
            }

            try {
                String sql = "INSERT INTO mouvement_caisse (caisse_id, type, motif, montant) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, caisseId);
                    ps.setString(2, type);
                    ps.setString(3, motif);
                    ps.setDouble(4, montant);
                    ps.executeUpdate();
                }
                dialog.dispose();
                chargerMouvements();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Erreur ajout mouvement : " + ex.getMessage());
            }
        });

        btnAnnuler.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }
}
