package org.example;

import java.awt.*;
import java.sql.Connection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class Paneloperateur extends JPanel {
    private final Connection conn;
    private DefaultTableModel tab;
    private JTable table;


    public Paneloperateur(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout());

        String[] Colonne = {"Nom operateur", "Groupe"};
        tab = new DefaultTableModel(Colonne, 0);
        table = new JTable(tab);
        add(new JScrollPane(table));

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnaj = new JButton("Ajouter");
        panel.add(btnaj);
        JButton btnch = new JButton("Changer operateur");
        panel.add(btnch);
        JButton btndel = new JButton("Supprimer");
        panel.add(btndel);
        btndel.addActionListener(e -> supprimer());

        btnaj.addActionListener(e -> {
            try {
                ajouteropp();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        btnch.addActionListener(e -> afficherop());
        add(panel, BorderLayout.NORTH);
        afficherop();
    }

    public void afficherop() {
        tab.setRowCount(0);
        try {
            String sql = "SELECT nom_operateur,groupe.nom_groupe FROM operateur LEFT JOIN groupe ON operateur.groupe_op=groupe.id ";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();


            while (rs.next()) {

                tab.addRow(new Object[]{

                        rs.getString("nom_operateur"),
                        rs.getString("nom_groupe")
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public void supprimer(){
        String sql="DELETE FROM operateur WHERE nom_operateur=?";
        String nom = JOptionPane.showInputDialog(this,"Saisir le nom de l'operateur");
        try(PreparedStatement ps=conn.prepareStatement(sql)){
            ps.setString(1,nom);
            ps.executeUpdate();
            afficherop();
        }catch(SQLException ex){
            throw new RuntimeException(ex);
        }
    }

    public void ajouteropp() throws SQLException {
        String sql = "insert into operateur (nom_operateur,password,groupe_op) values(?,?,?)";
        String nom = JOptionPane.showInputDialog(this, "Entrez le nom de l'operateur' :");

        if (nom == null) {
            JOptionPane.showMessageDialog(this, "Nom invalide ou vide.");


        } else {
            String pass = JOptionPane.showInputDialog(this, "Entrez le mot de passe :");
            String[] role3 = {"Admin", "Caissier", "Serveur"};
            String role = (String) JOptionPane.showInputDialog(this, "Choisissez le role :", "Sélection du rôle",
                    JOptionPane.QUESTION_MESSAGE,
                    null, role3, role3[0]);
            try {
                String sql1 = "SELECT id FROM groupe WHERE nom_groupe=?";
                int groupeid = -1;
                try (PreparedStatement ps = conn.prepareStatement(sql1)) {
                    ps.setString(1, role);

                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        groupeid = rs.getInt("id");
                        afficherop();
                    } else {
                        JOptionPane.showMessageDialog(this, "Rôle invalide dans la base de données.");

                    }

                }

            String sq="SELECT count(*) from operateur where nom_operateur=?";
            try (PreparedStatement ps = conn.prepareStatement(sq)) {
                ps.setString(1, nom);
                ResultSet rs = ps.executeQuery();
                if (rs.next()&& rs.getInt(1)>0) {
                    JOptionPane.showMessageDialog(this, "Operateur existe déjà");

                }
            }

            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nom);
                ps.setString(2, pass);
                ps.setInt(3,groupeid);
                ps.executeUpdate();
                afficherop();



            }


          } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
}
}
