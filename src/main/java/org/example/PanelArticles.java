package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;

public class PanelArticles extends JPanel {

    private DefaultTableModel model;
    private JTable table;
    private Connection conn;

    public PanelArticles(Connection conn) {
        this.conn = conn;
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAjouter = new JButton("Ajouter");
        JButton btnModifier = new JButton("Modifier");
        JButton btnSupprimer = new JButton("Supprimer");
        JButton btnRafraichir = new JButton("Rafraîchir");
        JButton btnAjouterFamille = new JButton("Ajouter une famille");

        topPanel.add(btnAjouter);
        topPanel.add(btnModifier);
        topPanel.add(btnAjouterFamille);
        topPanel.add(btnSupprimer);
        topPanel.add(btnRafraichir);
        add(topPanel, BorderLayout.NORTH);

        String[] colonnes = {"ID", "Nom Article", "Prix", "Prix supplementaires","Famille"};
        model = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.removeColumn(table.getColumnModel().getColumn(0));
        add(new JScrollPane(table), BorderLayout.CENTER);


        btnRafraichir.addActionListener(e -> chargerArticles());
        btnAjouter.addActionListener(e -> ouvrirFormulaire(null));
        btnAjouterFamille.addActionListener(e -> ajoutFamille());
        btnModifier.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Veuillez sélectionner un article à modifier.");
                return;
            }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(selectedRow), 0);
            ouvrirFormulaire(id);
        });
        btnSupprimer.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Veuillez sélectionner un article à supprimer.");
                return;
            }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(selectedRow), 0);
            supprimerArticle(id);
        });


        chargerArticles();
    }

    private void chargerArticles() {
        model.setRowCount(0);

        String sqlArticles = "SELECT a.id, a.nom_article, a.prix, f.nom_famille " +
                "FROM article a LEFT JOIN famille f ON a.famille_id = f.id";

        try (PreparedStatement ps = conn.prepareStatement(sqlArticles)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int articleId = rs.getInt("id");
                String nom = rs.getString("nom_article");
                double prixPrincipal = rs.getDouble("prix");
                String famille = rs.getString("nom_famille");


                String sqlPrixSup = "SELECT prix FROM article_prix WHERE article_id = ?";
                PreparedStatement psPrix = conn.prepareStatement(sqlPrixSup);
                psPrix.setInt(1, articleId);
                ResultSet rsPrix = psPrix.executeQuery();

                StringBuilder prixSupStr = new StringBuilder();
                int i = 1;
                while (rsPrix.next()) {
                    prixSupStr.append("PVP").append(i+1).append(" :").append(rsPrix.getDouble("prix")).append("   |");
                    i++;
                }

                Object[] row = {
                        articleId,
                        nom,
                        prixPrincipal,
                        prixSupStr.toString().trim(), // Prix supplémentaires
                        famille
                };
                model.addRow(row);

                rsPrix.close();
                psPrix.close();
            }

            rs.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement articles : " + e.getMessage());
        }
    }


    private void ajoutFamille() {
        String nom = JOptionPane.showInputDialog(this, "Entrez le nom de la nouvelle famille :");
        if (nom == null || nom.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nom invalide ou vide.");
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO famille (nom_famille) VALUES (?)")) {
            ps.setString(1, nom.trim());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Famille ajoutée avec succès !");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    private void ouvrirFormulaire(Integer idArticle) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), true);
        dialog.setTitle(idArticle == null ? "Ajouter Article" : "Modifier Article");
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel panelForm = new JPanel(new GridLayout(0, 2, 10, 10));

        JTextField tfNom = new JTextField();
        JComboBox<String> comboFamille = new JComboBox<>();


        JPanel panelPrix = new JPanel();
        panelPrix.setLayout(new GridLayout(0, 2, 5, 5));


        JTextField tfPrix = new JTextField();
        panelPrix.add(tfPrix);

        try (PreparedStatement ps = conn.prepareStatement("SELECT id, nom_famille FROM famille")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comboFamille.addItem(rs.getInt("id") + " - " + rs.getString("nom_famille"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur chargement familles : " + e.getMessage());
        }


        final Integer articleId = idArticle;


        if (articleId != null) {
            String sql = "SELECT nom_article, prix, famille_id FROM article WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, articleId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    tfNom.setText(rs.getString("nom_article"));
                    tfPrix.setText(String.valueOf(rs.getDouble("prix")));
                    int famId = rs.getInt("famille_id");

                    for (int i = 0; i < comboFamille.getItemCount(); i++) {
                        if (comboFamille.getItemAt(i).startsWith(famId + " -")) {
                            comboFamille.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erreur chargement article : " + e.getMessage());
            }
        }

        panelForm.add(new JLabel("Nom Article :"));
        panelForm.add(tfNom);

        panelForm.add(new JLabel("Famille :"));
        panelForm.add(comboFamille);

        panelForm.add(new JLabel("Prix :"));
        panelForm.add(panelPrix);


        JPanel panelBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnEnregistrer = new JButton(articleId == null ? "Ajouter l'article" : "Modifier l'article");
        JButton btnAnnuler = new JButton("Annuler");
        JButton btnPrix = new JButton("Ajouter prix");

        panelBtns.add(btnPrix);
        panelBtns.add(btnEnregistrer);
        panelBtns.add(btnAnnuler);


        btnPrix.addActionListener(e -> {
            int count =panelPrix.getComponentCount()/2;
            String texprix="PVP"+(count+1);
            JLabel lblPrix = new JLabel(texprix);
            JTextField nouveauPrix = new JTextField();
            panelPrix.add(lblPrix);
            panelPrix.add(nouveauPrix);
            panelPrix.revalidate();
            panelPrix.repaint();
        });


        btnEnregistrer.addActionListener(e -> {
            String nom = tfNom.getText().trim();
            if (nom.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Veuillez saisir un nom d'article.");
                return;
            }


            java.util.List<Double> prixList = new ArrayList<>();
            for (Component c : panelPrix.getComponents()) {
                if (c instanceof JTextField) {
                    String val = ((JTextField) c).getText().trim();
                    if (!val.isEmpty()) {
                        try {
                            prixList.add(Double.parseDouble(val));
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(dialog, "Prix invalide : " + val);
                            return;
                        }
                    }
                }
            }

            if (prixList.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Veuillez saisir au moins un prix.");
                return;
            }

            int familleId = Integer.parseInt(comboFamille.getSelectedItem().toString().split(" - ")[0]);

            try {
                if (articleId == null) {

                    String sql = "INSERT INTO article (nom_article, prix, famille_id) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, nom);
                        ps.setDouble(2, prixList.get(0));
                        ps.setInt(3, familleId);
                        ps.executeUpdate();


                        ResultSet keys = ps.getGeneratedKeys();
                        if (keys.next()) {
                            int newId = keys.getInt(1);

                            for (int i = 1; i < prixList.size(); i++) {
                                try (PreparedStatement psPrix = conn.prepareStatement(
                                        "INSERT INTO article_prix (article_id, prix) VALUES (?, ?)")) {
                                    psPrix.setInt(1, newId);
                                    psPrix.setDouble(2, prixList.get(i));
                                    psPrix.executeUpdate();
                                }
                            }
                        }
                    }
                } else {
                    String sql = "UPDATE article SET nom_article = ?, prix = ?, famille_id = ? WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, nom);
                        ps.setDouble(2, prixList.get(0)); // prix principal
                        ps.setInt(3, familleId);
                        ps.setInt(4, articleId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement psDel = conn.prepareStatement("DELETE FROM article_prix WHERE article_id = ?")) {
                        psDel.setInt(1, articleId);
                        psDel.executeUpdate();
                    }
                    for (int i = 1; i < prixList.size(); i++) {
                        try (PreparedStatement psPrix = conn.prepareStatement(
                                "INSERT INTO article_prix (article_id, prix) VALUES (?, ?)")) {
                            psPrix.setInt(1, articleId);
                            psPrix.setDouble(2, prixList.get(i));
                            psPrix.executeUpdate();
                        }
                    }
                }

                dialog.dispose();
                chargerArticles();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(dialog, "Erreur sauvegarde article : " + ex.getMessage());
            }
        });

        btnAnnuler.addActionListener(e -> dialog.dispose());

        dialog.add(panelForm, BorderLayout.CENTER);
        dialog.add(panelBtns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }



    private void supprimerArticle(int idArticle) {
        int choix = JOptionPane.showConfirmDialog(this,
                "Voulez-vous vraiment supprimer cet article ?",
                "Confirmer suppression",
                JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) return;

        try {
            String sql = "DELETE FROM article WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idArticle);
                ps.executeUpdate();
            }
            chargerArticles();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur suppression article : " + e.getMessage());
        }
    }
}
