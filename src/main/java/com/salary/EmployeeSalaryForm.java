package com.salary;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class EmployeeSalaryForm extends JFrame {

    private JTextField txtEmployeeId;
    private JTextField txtBaseSalary;
    private JTextField txtHousing;
    private JTextField txtTransport;
    private JTextField txtTaxable;

    public EmployeeSalaryForm() {

        setTitle("Employee Salary Registration");
        setSize(500, 380);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));

        panel.setBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Employee ID:"));
        txtEmployeeId = new JTextField();
        panel.add(txtEmployeeId);

        panel.add(new JLabel("Basic Salary :"));
        txtBaseSalary = new JTextField();
        panel.add(txtBaseSalary);

        panel.add(new JLabel("Housing Allowance :"));
        txtHousing = new JTextField();
        panel.add(txtHousing);

        panel.add(new JLabel("Transport Allowance :"));
        txtTransport = new JTextField();
        panel.add(txtTransport);

        panel.add(new JLabel("Taxable Allowance :"));
        txtTaxable = new JTextField();
        panel.add(txtTaxable);

        JButton btnSave = new JButton("Save");

        panel.add(new JLabel(""));
        panel.add(btnSave);

        add(panel);

        btnSave.addActionListener(e -> saveEmployee());
    }

    private void saveEmployee() {

        try {

            String employeeId = txtEmployeeId.getText().trim();

            if (employeeId.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this, "Please enter Employee ID.");
                return;
            }

            long basicSalary = readAmount(txtBaseSalary);
            long housing = readAmount(txtHousing);
            long transport = readAmount(txtTransport);
            long taxable = readAmount(txtTaxable);

            // 1. Calculate gross salary
            long grossSalary = Math.addExact(
                    Math.addExact(basicSalary, housing),
                    Math.addExact(transport, taxable));

                        // 2. Deductions (standard Rwanda payroll rules)
            long pension   = Math.round(grossSalary * 0.06);    // 6% of gross
            long maternity = Math.round(grossSalary * 0.003);   // 0.3% of gross
            long medical   = Math.round(basicSalary * 0.075);   // 7.5% of basic salary

            // 3. PAYE (monthly bands, applied progressively to gross)
            long paye;
            if (grossSalary <= 60000) {
                paye = 0;
            } else if (grossSalary <= 100000) {
                paye = Math.round((grossSalary - 60000) * 0.10);
            } else if (grossSalary <= 200000) {
                paye = Math.round(4000 + (grossSalary - 100000) * 0.20);
            } else {
                paye = Math.round(24000 + (grossSalary - 200000) * 0.30);
            }

            // 4. CBHI = 0.5% of the salary left after the other deductions
            long cbhi = Math.round(
                    (grossSalary - pension - maternity - medical - paye) * 0.005);

            long totalDeductions = pension + maternity + medical + paye + cbhi;
            long netSalary = grossSalary - totalDeductions;

            // 5. Save calculated values to MySQL
            String sql = "INSERT INTO employee2_salary "
                    + "(employee_id, basic_salary, housing_allowance, "
                    + "transport_allowance, taxable_allowance, gross_salary, "
                    + "pension, maternity_leave, medical_insurance, "
                    + "paye, cbhi, net_salary) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection connection =
                         DBConnection.getConnection();

                 PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setString(1, employeeId);
                statement.setLong(2, basicSalary);
                statement.setLong(3, housing);
                statement.setLong(4, transport);
                statement.setLong(5, taxable);
                statement.setLong(6, grossSalary);
                statement.setLong(7, pension);
                statement.setLong(8, maternity);
                statement.setLong(9, medical);
                statement.setLong(10, paye);
                statement.setLong(11, cbhi);
                statement.setLong(12, netSalary);

                statement.executeUpdate();

                JOptionPane.showMessageDialog(
                        this,
                        "EMPLOYEE SALARY CALCULATION\n\n"
                        + "Gross Salary: " + grossSalary + " RWF\n"
                        + "Pension: " + pension + " RWF\n"
                        + "Maternity Leave: " + maternity + " RWF\n"
                        + "Medical Insurance: " + medical + " RWF\n"
                        + "PAYE: " + paye + " RWF\n"
                        + "CBHI: " + cbhi + " RWF\n"
                        + "Total Deductions: " + totalDeductions + " RWF\n"
                        + "Net Salary: " + netSalary + " RWF\n\n"
                        + "Employee saved successfully!");

                clearFields();
            }

        } catch (NumberFormatException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter valid whole numbers for all salary fields.");

        } catch (ArithmeticException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "The salary amount is too large.");

        } catch (SQLException ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Database error: " + ex.getMessage());
        }
    }

    private long readAmount(JTextField field) {

        String value = field.getText().trim();

        if (value.isEmpty()) {
            throw new NumberFormatException(
                    "Salary field cannot be empty.");
        }

        long amount = Long.parseLong(value);

        if (amount < 0) {
            throw new NumberFormatException(
                    "Salary cannot be negative.");
        }

        return amount;
    }

    private void clearFields() {

        txtEmployeeId.setText("");
        txtBaseSalary.setText("");
        txtHousing.setText("");
        txtTransport.setText("");
        txtTaxable.setText("");

        txtEmployeeId.requestFocus();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            EmployeeSalaryForm form =
                    new EmployeeSalaryForm();

            form.setVisible(true);
        });
    }
}
