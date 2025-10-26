/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Latihan3.view;

import Latihan3.controller.KontakController;
import Latihan3.model.Kontak;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.SQLException;
import java.util.List;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PengelolaanKontakFrame extends javax.swing.JFrame {

    private DefaultTableModel model;
    private KontakController controller;

   public PengelolaanKontakFrame() {
    initComponents();
    controller = new KontakController();

    // 🟢 Tambahkan listener pencarian
    txtPencarian.addKeyListener(new java.awt.event.KeyAdapter() {
        @Override
        public void keyReleased(java.awt.event.KeyEvent evt) {
            searchContact();
        }
    }); // ✅ tutup di sini, jangan taruh method lain di dalamnya

    // Inisialisasi tabel
    model = new DefaultTableModel(new String[]{"No", "Nama", "Nomor Telepon", "Kategori"}, 0);
    tblKontak.setModel(model);
    loadContacts();
}
private void exportToCSV() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Simpan File CSV");
    int userSelection = fileChooser.showSaveDialog(this);
    if (userSelection == JFileChooser.APPROVE_OPTION) {
        File fileToSave = fileChooser.getSelectedFile();

        // Tambahkan ekstensi .csv jika belum ada
        if (!fileToSave.getAbsolutePath().endsWith(".csv")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
            writer.write("ID,Nama,Nomor Telepon,Kategori\n"); // Header CSV
            for (int i = 0; i < model.getRowCount(); i++) {
                writer.write(
                    model.getValueAt(i, 0) + "," +
                    model.getValueAt(i, 1) + "," +
                    model.getValueAt(i, 2) + "," +
                    model.getValueAt(i, 3) + "\n"
                );
            }
            JOptionPane.showMessageDialog(this, "Data berhasil diekspor ke " + fileToSave.getAbsolutePath());
        } catch (IOException ex) {
            showError("Gagal menulis file: " + ex.getMessage());
        }
    }
}
private void importFromCSV() {
    showCSVGuide();
    int confirm = JOptionPane.showConfirmDialog(
        this,
        "Apakah Anda yakin file CSV yang dipilih sudah sesuai dengan format?",
        "Konfirmasi Impor CSV",
        JOptionPane.YES_NO_OPTION
    );

    if (confirm == JOptionPane.YES_OPTION) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Pilih File CSV");
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();

            try (BufferedReader reader = new BufferedReader(new FileReader(fileToOpen))) {
                String line = reader.readLine(); // Baca header
                if (!validateCSVHeader(line)) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Format header CSV tidak valid.\nPastikan header adalah: ID,Nama,Nomor Telepon,Kategori",
                        "Kesalahan CSV",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                int rowCount = 0;
                int errorCount = 0;
                int duplicateCount = 0;
                StringBuilder errorLog = new StringBuilder("Baris dengan kesalahan:\n");

                while ((line = reader.readLine()) != null) {
                    rowCount++;
                    String[] data = line.split(",");

                    if (data.length != 4) {
                        errorCount++;
                        errorLog.append("Baris ").append(rowCount + 1).append(": Format kolom tidak sesuai.\n");
                        continue;
                    }

                    String nama = data[1].trim();
                    String nomorTelepon = data[2].trim();
                    String kategori = data[3].trim();

                    if (nama.isEmpty() || nomorTelepon.isEmpty()) {
                        errorCount++;
                        errorLog.append("Baris ").append(rowCount + 1).append(": Nama atau Nomor Telepon kosong.\n");
                        continue;
                    }

                    if (!validatePhoneNumber(nomorTelepon)) {
                        errorCount++;
                        errorLog.append("Baris ").append(rowCount + 1).append(": Nomor Telepon tidak valid.\n");
                        continue;
                    }

                    try {
                        if (controller.isDuplicatePhoneNumber(nomorTelepon, null)) {
                            duplicateCount++;
                            errorLog.append("Baris ").append(rowCount + 1).append(": Kontak sudah ada.\n");
                            continue;
                        }
                    } catch (SQLException ex) {
                        Logger.getLogger(PengelolaanKontakFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    try {
                        controller.addContact(nama, nomorTelepon, kategori);
                    } catch (SQLException ex) {
                        errorCount++;
                        errorLog.append("Baris ").append(rowCount + 1)
                                .append(": Gagal menyimpan ke database - ")
                                .append(ex.getMessage()).append("\n");
                    }
                }

                loadContacts();

                if (errorCount > 0 || duplicateCount > 0) {
                    errorLog.append("\nTotal baris dengan kesalahan: ").append(errorCount).append("\n");
                    errorLog.append("Total baris duplikat: ").append(duplicateCount).append("\n");
                    JOptionPane.showMessageDialog(this, errorLog.toString(), "Kesalahan Impor", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Semua data berhasil diimpor.");
                }

            } catch (IOException ex) {
                showError("Gagal membaca file: " + ex.getMessage());
            }
        }
    }
}
private void showCSVGuide() {
    String guideMessage = """
            Format CSV untuk impor data:
            - Header wajib: ID,Nama,Nomor Telepon,Kategori
            - ID dapat kosong (akan diisi otomatis)
            - Nama dan Nomor Telepon wajib diisi
            - Contoh isi file CSV:
              1, Andi, 08123456789, Teman
              2, Budi Doremi, 08567890123, Keluarga

            Pastikan file CSV sesuai format sebelum melakukan impor.
            """;
    JOptionPane.showMessageDialog(this, guideMessage, "Panduan Format CSV", JOptionPane.INFORMATION_MESSAGE);
}

private boolean validateCSVHeader(String header) {
    return header != null && header.trim().equalsIgnoreCase("ID,Nama,Nomor Telepon,Kategori");
}
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        lblJudul = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtNama = new javax.swing.JTextField();
        txtNomorTelepon = new javax.swing.JTextField();
        cmbkategori = new javax.swing.JComboBox<>();
        txtPencarian = new javax.swing.JTextField();
        btnTambah = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblKontak = new javax.swing.JTable();
        btnExport = new javax.swing.JButton();
        btnImport = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("AplikasiPengelolaanKontak");

        lblJudul.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        lblJudul.setText("Aplikasi Pengelolaan Kontak");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(195, Short.MAX_VALUE)
                .addComponent(lblJudul)
                .addGap(100, 100, 100))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(35, Short.MAX_VALUE)
                .addComponent(lblJudul)
                .addContainerGap())
        );

        jLabel2.setText("Nama Kontak");

        jLabel3.setText("Nomor Telepon");

        jLabel4.setText("Kategori");

        jLabel5.setText("Pencarian");

        txtNama.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNamaActionPerformed(evt);
            }
        });

        cmbkategori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Keluarga", "Teman", "Kantor" }));

        btnTambah.setText("Tambah");
        btnTambah.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTambahActionPerformed(evt);
            }
        });

        btnEdit.setText("Edit");
        btnEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditActionPerformed(evt);
            }
        });

        btnHapus.setText("Hapus");
        btnHapus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHapusActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(56, 56, 56)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(177, 177, 177)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(txtNama)
                                    .addComponent(txtNomorTelepon, javax.swing.GroupLayout.DEFAULT_SIZE, 267, Short.MAX_VALUE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(cmbkategori, javax.swing.GroupLayout.PREFERRED_SIZE, 267, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(178, 178, 178)
                                .addComponent(txtPencarian, javax.swing.GroupLayout.PREFERRED_SIZE, 266, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(btnTambah)
                        .addGap(18, 18, 18)
                        .addComponent(btnEdit)
                        .addGap(18, 18, 18)
                        .addComponent(btnHapus)))
                .addContainerGap(55, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtNama, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(19, 19, 19)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtNomorTelepon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(21, 21, 21)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(cmbkategori, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnTambah)
                    .addComponent(btnEdit)
                    .addComponent(btnHapus))
                .addGap(21, 21, 21)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtPencarian, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addContainerGap(28, Short.MAX_VALUE))
        );

        tblKontak.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tblKontak.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblKontakMouseClicked(evt);
            }
        });
        tblKontak.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                tblKontakKeyTyped(evt);
            }
        });
        jScrollPane1.setViewportView(tblKontak);

        btnExport.setText("Export");
        btnExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportActionPerformed(evt);
            }
        });

        btnImport.setText("Import");
        btnImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnImportActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 25, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(layout.createSequentialGroup()
                            .addGap(48, 48, 48)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 535, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(btnExport)
                        .addGap(30, 30, 30)
                        .addComponent(btnImport)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnImport)
                    .addComponent(btnExport))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnTambahActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTambahActionPerformed
      addContact();
    }//GEN-LAST:event_btnTambahActionPerformed

    private void txtNamaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNamaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNamaActionPerformed

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
        editContact();

    }//GEN-LAST:event_btnEditActionPerformed

    private void tblKontakMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblKontakMouseClicked
      int selectedRow = tblKontak.getSelectedRow();
        if (selectedRow != -1) {
    populateInputFields(selectedRow);
        }
    }//GEN-LAST:event_tblKontakMouseClicked

    private void btnHapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusActionPerformed
        deleteContact();
    }//GEN-LAST:event_btnHapusActionPerformed
    private void tblKontakKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tblKontakKeyTyped
        searchContact();
    }//GEN-LAST:event_tblKontakKeyTyped

    private void btnExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportActionPerformed
        exportToCSV();
    }//GEN-LAST:event_btnExportActionPerformed
    private void btnImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnImportActionPerformed
        importFromCSV();
    }//GEN-LAST:event_btnImportActionPerformed

    /**
     * @param args the command line arguments
     */
   public static void main(String args[]) {
    java.awt.EventQueue.invokeLater(() -> new PengelolaanKontakFrame().setVisible(true));
}
   
   
   
   
     private void deleteContact() {
        int selectedRow = tblKontak.getSelectedRow();
        if (selectedRow != -1) {
            // ambil id dari daftar kontak yang diambil dari controller agar konsisten dengan DB
            try {
                int id = controller.getAllContacts().get(selectedRow).getId();
                controller.deleteContact(id);
                loadContacts();
                JOptionPane.showMessageDialog(this, "Kontak berhasil dihapus!");
                clearInputFields();
            } catch (SQLException e) {
                showError(e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Pilih kontak yang ingin dihapus.", "Kesalahan", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void searchContact() {
        String keyword = txtPencarian.getText().trim();
        if (!keyword.isEmpty()) {
            try {
                List<Kontak> contacts = controller.searchContacts(keyword);
                model.setRowCount(0); // Bersihkan tabel
                int no = 1;
                for (Kontak contact : contacts) {
                    model.addRow(new Object[]{
                        no++,
                        contact.getNama(),
                        contact.getNomorTelepon(),
                        contact.getKategori()
                    });
                }
                if (contacts.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Tidak ada kontak ditemukan.");
                }
            } catch (SQLException ex) {
                showError(ex.getMessage());
            }
        } else {
            loadContacts();
        }
    }

    private void editContact() {
        int selectedRow = tblKontak.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih kontak yang ingin diperbarui.", "Kesalahan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ambil id nyata dari DB via controller (sesuaikan indeks dengan urutan yang ditampilkan)
        try {
            int id = controller.getAllContacts().get(selectedRow).getId();
            String nama = txtNama.getText().trim();
            String nomorTelepon = txtNomorTelepon.getText().trim();
            String kategori = (String) cmbkategori.getSelectedItem();

            if (!validatePhoneNumber(nomorTelepon)) return;

            if (controller.isDuplicatePhoneNumber(nomorTelepon, id)) {
                JOptionPane.showMessageDialog(this, "Kontak nomor telepon ini sudah ada.", "Kesalahan", JOptionPane.WARNING_MESSAGE);
                return;
            }
            controller.updateContact(id, nama, nomorTelepon, kategori);
            loadContacts();
            JOptionPane.showMessageDialog(this, "Kontak berhasil diperbarui!");
            clearInputFields();
        } catch (SQLException ex) {
            showError("Gagal memperbarui kontak: " + ex.getMessage());
        }
    }

    private void populateInputFields(int selectedRow) {
        // Ambil data dari JTable (kolom sesuai model: No, Nama, Nomor, Kategori)
        String nama = model.getValueAt(selectedRow, 1).toString();
        String nomorTelepon = model.getValueAt(selectedRow, 2).toString();
        String kategori = model.getValueAt(selectedRow, 3).toString();

        // Set data ke komponen input
        txtNama.setText(nama);
        txtNomorTelepon.setText(nomorTelepon);
        cmbkategori.setSelectedItem(kategori);
    }

    private void loadContacts() {
        try {
            model.setRowCount(0);
            List<Kontak> contacts = controller.getAllContacts();
            int rowNumber = 1;
            for (Kontak contact : contacts) {
                model.addRow(new Object[]{
                    rowNumber++,
                    contact.getNama(),
                    contact.getNomorTelepon(),
                    contact.getKategori()
                });
            }
        } catch (SQLException e) {
            showError(e.getMessage());
        }
    }

    private void addContact() {
        String nama = txtNama.getText().trim();
        String nomorTelepon = txtNomorTelepon.getText().trim();
        String kategori = (String) cmbkategori.getSelectedItem();

        if (!validatePhoneNumber(nomorTelepon)) {
            return;
        }
        try {
            if (controller.isDuplicatePhoneNumber(nomorTelepon, null)) {
                JOptionPane.showMessageDialog(this, "Kontak nomor telepon ini sudah ada.", "Kesalahan", JOptionPane.WARNING_MESSAGE);
                return;
            }

            controller.addContact(nama, nomorTelepon, kategori);
            loadContacts();
            JOptionPane.showMessageDialog(this, "Kontak berhasil ditambahkan!");
            clearInputFields();
        } catch (SQLException ex) {
            showError("Gagal menambahkan kontak: " + ex.getMessage());
        }
    }

    private boolean validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nomor telepon tidak boleh kosong.");
            return false;
        }
        if (!phoneNumber.matches("\\d+")) {
            JOptionPane.showMessageDialog(this, "Nomor telepon hanya boleh berisi angka.");
            return false;
        }
        if (phoneNumber.length() < 8 || phoneNumber.length() > 15) {
            JOptionPane.showMessageDialog(this, "Nomor telepon harus memiliki panjang antara 8 hingga 15 karakter.");
            return false;
        }
        return true;
    }

    private void clearInputFields() {
        txtNama.setText("");
        txtNomorTelepon.setText("");
        cmbkategori.setSelectedIndex(0);
    }

    // method untuk menampilkan pesan error
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnExport;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnImport;
    private javax.swing.JButton btnTambah;
    private javax.swing.JComboBox<String> cmbkategori;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblJudul;
    private javax.swing.JTable tblKontak;
    private javax.swing.JTextField txtNama;
    private javax.swing.JTextField txtNomorTelepon;
    private javax.swing.JTextField txtPencarian;
    // End of variables declaration//GEN-END:variables
}
