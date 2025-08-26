package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class PanelClients extends JPanel {

    private DefaultTableModel model;
    private JTable table;
    private Connection conn;

    public PanelClients(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(10, 10));


        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAjouter = new JButton("Ajouter");
        JButton btnModifier = new JButton("Modifier");
        JButton btnajouttype = new JButton("Ajouter type client");
        JButton btnSupprimer = new JButton("Supprimer");
        JButton btnRafraichir = new JButton("Rafraîchir");
        topPanel.add(btnAjouter);
        topPanel.add(btnModifier);
        topPanel.add(btnajouttype);
        topPanel.add(btnSupprimer);
        topPanel.add(btnRafraichir);
        add(topPanel, BorderLayout.NORTH);

        String[] colonnes = {"ID", "Nom Client", "Type Client"};
        model = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table = new JTable(model);
        table.removeColumn(table.getColumnModel().getColumn(0));
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnRafraichir.addActionListener(e -> chargerClients());
        btnajouttype.addActionListener(e -> ajoutertype());
        btnAjouter.addActionListener(e -> ouvrirFormulaire(null));
        btnModifier.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Veuillez sélectionner un client à modifier.");
                return;
            }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(selectedRow), 0);
            ouvrirFormulaire(id);
        });
        btnSupprimer.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Veuillez sélectionner un client à supprimer.");
                return;
            }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(selectedRow), 0);
            supprimerClient(id);
        });


        chargerClients();
    }
private void ajoutertype(){
        String type= JOptionPane.showInputDialog("Entrer le type de client ");
        try(PreparedStatement ps= conn.prepareStatement("INSERT  INTO type_client (nom_type) VALUES (?)")){
            ps.setString(1,type.trim());
            ps.executeUpdate();

        }catch(SQLException e){
            JOptionPane.showMessageDialog(this, e);
        }
}
    private void chargerClients() {
        model.setRowCount(0);
        String sql = "SELECT c.id, c.nom_client, t.nom_type FROM client c " +
                "LEFT JOIN type_client t ON c.id_type = t.id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Object[] row = {
                        rs.getInt("id"),
                        rs.getString("nom_client"),
                        rs.getString("nom_type")
                };
                model.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement clients : " + e.getMessage());
        }
    }

    private void ouvrirFormulaire(Integer idClient) {

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        dialog.setTitle(idClient == null ? "Ajouter Client" : "Modifier Client");
        dialog.setSize(350, 200);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridLayout(3, 2, 10, 10));

        JTextField tfNom = new JTextField();
        JComboBox<String> comboType = new JComboBox<>();

        try (PreparedStatement ps = conn.prepareStatement("SELECT id, nom_type FROM type_client")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comboType.addItem(rs.getInt("id") + " - " + rs.getString("nom_type"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement types client : " + e.getMessage());
        }

        if (idClient != null) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT nom_client, id_type FROM client WHERE id = ?")) {
                ps.setInt(1, idClient);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    tfNom.setText(rs.getString("nom_client"));
                    int typeId = rs.getInt("id_type");
                    for (int i = 0; i < comboType.getItemCount(); i++) {
                        if (comboType.getItemAt(i).startsWith(typeId + " -")) {
                            comboType.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erreur chargement client : " + e.getMessage());
            }
        }

        dialog.add(new JLabel("Nom Client :"));
        dialog.add(tfNom);
        dialog.add(new JLabel("Type Client :"));
        dialog.add(comboType);

        JButton btnEnregistrer = new JButton(idClient == null ? "Ajouter" : "Modifier");
        JButton btnAnnuler = new JButton("Annuler");
        dialog.add(btnEnregistrer);
        dialog.add(btnAnnuler);

        btnEnregistrer.addActionListener(e -> {
            String nom = tfNom.getText().trim();
            if (nom.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Veuillez saisir le nom du client.");
                return;
            }
            int typeId = Integer.parseInt(comboType.getSelectedItem().toString().split(" - ")[0]);

            try {
                if (idClient == null) {
                    String sql = "INSERT INTO client (nom_client, id_type) VALUES (?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, nom);
                        ps.setInt(2, typeId);
                        ps.executeUpdate();
                    }
                } else {
                    String sql = "UPDATE client SET nom_client = ?, id_type = ? WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, nom);
                        ps.setInt(2, typeId);
                        ps.setInt(3, idClient);
                        ps.executeUpdate();
                    }
                }
                dialog.dispose();
                chargerClients();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Erreur sauvegarde client : " + ex.getMessage());
            }
        });

        btnAnnuler.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void supprimerClient(int idClient) {
        int choix = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment supprimer ce client ?",
                "Confirmer suppression",
                JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) return;

        try {
            String sql = "DELETE FROM client WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idClient);
                ps.executeUpdate();
            }
            chargerClients();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur suppression client : " + e.getMessage());
        }
    }
}
