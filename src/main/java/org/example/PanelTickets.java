
package org.example;

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

        setLayout(new BorderLayout(10,10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRafraichir = new JButton("Rafraîchir");
        topPanel.add(btnRafraichir);
        add(topPanel, BorderLayout.NORTH);

        String[] colonnes = {"ID", "Date/Heure", "Client", "Table", "Opérateur", "Total TTC"};
        model = new DefaultTableModel(colonnes,0){
            @Override
            public boolean isCellEditable(int row, int column) {

                return false;
            }
        };
        table = new JTable(model);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnRafraichir.addActionListener(e -> chargerTickets());


        chargerTickets();
    }

    void chargerTickets() {
        model.setRowCount(0);
       String sql= " SELECT t.id, t.date_heure, c.nom_client, lt.nom_table, o.nom_operateur, SUM(cde.prix_total) AS total_ttc FROM ticket t LEFT JOIN client c ON t.client_id = c.id LEFT JOIN les_tables lt ON t.table_id = lt.id LEFT JOIN operateur o ON t.operateur_id = o.id LEFT JOIN commande cde ON cde.ticket_id = t.id GROUP BY t.id, t.date_heure, c.nom_client, lt.nom_table, o.nom_operateur ORDER BY t.date_heure asc " ;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            while (rs.next()) {
                Timestamp dt = rs.getTimestamp("date_heure");
                String dateStr = dt != null ? sdf.format(dt) : "";
                Object[] row = {
                        rs.getInt("id"),
                        dateStr,
                        rs.getString("nom_client"),
                        rs.getString("nom_table"),
                        rs.getString("nom_operateur"),
                        rs.getDouble("total_ttc")
                };

                    model.addRow(row);


            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement tickets : " + e.getMessage());
        }
    }

    }
