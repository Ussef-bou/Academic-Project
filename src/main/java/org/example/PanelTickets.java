package org.example;

import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;

public class PanelTickets extends JPanel {

    private DefaultTableModel model;
    private JTable table;
    private Connection conn;

    public PanelTickets(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(10, 10));

        // Panel supérieur pour les boutons et filtres
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRafraichir = new JButton("Rafraîchir");
        JDateChooser dateDebut = new JDateChooser();
        dateDebut.setDateFormatString("yyyy-MM-dd");
        JDateChooser dateFin = new JDateChooser();
        dateFin.setDateFormatString("yyyy-MM-dd");
        JTextField txtTable = new JTextField(5);
        JButton btnFiltrer = new JButton("Filtrer");

        topPanel.add(btnRafraichir);
        topPanel.add(new JLabel("Début:")); topPanel.add(dateDebut);
        topPanel.add(new JLabel("Fin:")); topPanel.add(dateFin);
        topPanel.add(new JLabel("Table:")); topPanel.add(txtTable);
        topPanel.add(btnFiltrer);

        add(topPanel, BorderLayout.NORTH);

        // Table des tickets
        String[] colonnes = {"ID", "Date/Heure", "Client", "Table", "Opérateur", "Total TTC"};
        model = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Actions
        btnRafraichir.addActionListener(e -> chargerTickets(null, null, null));
        btnFiltrer.addActionListener(e -> chargerTickets(dateDebut.getDate(), dateFin.getDate(), txtTable.getText()));

        // Double-clic pour détail ticket
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        int ticketId = (int) model.getValueAt(row, 0);
                        afficherDetailTicket(ticketId);
                    }
                }
            }
        });

        // Chargement initial
        chargerTickets(null, null, null);
    }

    private void chargerTickets(java.util.Date debut, java.util.Date fin, String tableNom) {
        model.setRowCount(0);
        String sql = "SELECT t.id, t.date_heure, c.nom_client, lt.nom_table, o.nom_operateur, SUM(cde.prix_total) AS total_ttc " +
                "FROM ticket t " +
                "LEFT JOIN client c ON t.client_id = c.id " +
                "LEFT JOIN les_tables lt ON t.table_id = lt.id " +
                "LEFT JOIN operateur o ON t.operateur_id = o.id " +
                "LEFT JOIN commande cde ON cde.ticket_id = t.id " +
                "WHERE 1=1";

        if (debut != null) sql += " AND DATE(t.date_heure) >= ?";
        if (fin != null) sql += " AND DATE(t.date_heure) <= ?";
        if (tableNom != null && !tableNom.isEmpty()) sql += " AND lt.nom_table LIKE ?";

        sql += " GROUP BY t.id, t.date_heure, c.nom_client, lt.nom_table, o.nom_operateur ORDER BY t.date_heure ASC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (debut != null) ps.setDate(idx++, new java.sql.Date(debut.getTime()));
            if (fin != null) ps.setDate(idx++, new java.sql.Date(fin.getTime()));
            if (tableNom != null && !tableNom.isEmpty()) ps.setString(idx++, "%" + tableNom + "%");

            ResultSet rs = ps.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            while (rs.next()) {
                Timestamp dt = rs.getTimestamp("date_heure");
                String dateStr = dt != null ? sdf.format(dt) : "";
                model.addRow(new Object[]{
                        rs.getInt("id"),
                        dateStr,
                        rs.getString("nom_client"),
                        rs.getString("nom_table"),
                        rs.getString("nom_operateur"),
                        rs.getDouble("total_ttc")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement tickets : " + e.getMessage());
        }
    }

    private void afficherDetailTicket(int ticketId) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Détails Ticket #" + ticketId);
        DefaultTableModel detailModel = new DefaultTableModel(new String[]{"Article", "Prix", "Quantité", "Total"}, 0);
        JTable detailTable = new JTable(detailModel);

        String sql = "SELECT a.nom_article, c.prix_unitaire, c.quantite, c.prix_total " +
                "FROM commande c " +
                "JOIN article a ON c.article_id = a.id " +
                "WHERE c.ticket_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                detailModel.addRow(new Object[]{
                        rs.getString("nom_article"),
                        rs.getDouble("prix_unitaire"),
                        rs.getInt("quantite"),
                        rs.getDouble("prix_total")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur détail ticket : " + e.getMessage());
        }

        dialog.add(new JScrollPane(detailTable));
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
}
