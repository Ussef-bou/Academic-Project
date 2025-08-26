package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class Main extends JFrame {




    public static void main(String[] args) {
        Connection conn = DatabaseConnection.connect();
        if (conn == null) {
            JOptionPane.showMessageDialog(null, "Erreur de connexion à la base de données.");
            return;
        }
        ImageIcon image = new ImageIcon(Main.class.getResource("/coda.jpg"));

        JFrame frame = new JFrame("Connexion");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(350, 250);
        frame.setLocationRelativeTo(null);
        frame.setIconImage(image.getImage());


        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titre = new JLabel("Connexion");
        titre.setFont(new Font("Arial", Font.BOLD, 20));

        JLabel nomLabel = new JLabel("Nom d'utilisateur:");
        JTextField nomField = new JTextField(15);

        JLabel passLabel = new JLabel("Mot de passe:");
        JPasswordField passField = new JPasswordField(15);

        JButton loginBtn = new JButton("Se connecter" );

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(titre, gbc);

        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(nomLabel, gbc);
        gbc.gridx = 1;
        panel.add(nomField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(passLabel, gbc);
        gbc.gridx = 1;
        panel.add(passField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(loginBtn, gbc);

        frame.add(panel);
        frame.setVisible(true);

        loginBtn.addActionListener(
                e -> {
            String username = nomField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Veuillez remplir tous les champs !");
                return;
            }

            try {
                String sql1="SELECT g.nom_groupe ,o.id FROM operateur o JOIN groupe g ON o.groupe_op=g.id WHERE o.nom_operateur=? AND o.password=?";
                String sql = "SELECT * FROM operateur WHERE nom_operateur = ? AND password = ?";
                PreparedStatement ps = conn.prepareStatement(sql1);
                ps.setString(1, username);
                ps.setString(2, password);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int operateurId = rs.getInt("id");
                    String nomGroupe = rs.getString("nom_groupe");
                    frame.dispose();
                    new Dashboard(conn,operateurId,nomGroupe);
                } else {
                    JOptionPane.showMessageDialog(frame, "Nom d'utilisateur ou mot de passe incorrect !");
                }

                rs.close();
                ps.close();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Erreur : " + ex.getMessage());
            }
        });
    }
}
