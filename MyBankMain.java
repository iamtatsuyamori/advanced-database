package sample;

import java.io.File;

public class MyBankMain {

  public static void main(String[] args) throws Exception {
    String scalarDBProperties = null;
    String action = null;
    String fromTable = null;
    String fromId = null;
    String toTable = null;
    String toId = null;
    String table = null;
    String id = null;
    int amount = 0;
    String date = null;

    for (int i = 0; i < args.length; ++i) {
      if ("-action".equals(args[i])) {
        action = args[++i];
      } else if ("-fromTable".equals(args[i])) {
        fromTable = args[++i];
      } else if ("-fromId".equals(args[i])) {
        fromId = args[++i];
      } else if ("-toTable".equals(args[i])) {
        toTable = args[++i];
      } else if ("-toId".equals(args[i])) {
        toId = args[++i];
      } else if ("-table".equals(args[i])) {
        table = args[++i];
      } else if ("-id".equals(args[i])) {
        id = args[++i];
      } else if ("-amount".equals(args[i])) {
        amount = Integer.parseInt(args[++i]);
      } else if ("-date".equals(args[i])) {
        date = args[++i];
      } else if ("-config".equals(args[i])) {
        scalarDBProperties = args[++i];
      } else if ("-help".equals(args[i])) {
        printUsageAndExit();
        return;
      }
    }

    if (action == null) {
      printUsageAndExit();
      return;
    }

    MyBank myBank;
    if (scalarDBProperties != null) {
      myBank = new MyBank(scalarDBProperties);
    } else {
      scalarDBProperties = System.getProperty("user.dir") + File.separator + "scalardb.properties";
      myBank = new MyBank(scalarDBProperties);
    }

    if (action.equalsIgnoreCase("deposit")) {
      if (table == null || id == null || amount < 0) {
        printUsageAndExit();
        return;
      }
      myBank.deposit(table, id, amount);
    } else if (action.equalsIgnoreCase("withdraw")) {
      if (table == null || id == null || amount < 0) {
        printUsageAndExit();
        return;
      }
      myBank.withdraw(table, id, amount);
    } else if (action.equalsIgnoreCase("transfer")) {
      if (fromTable == null || fromId == null || toTable == null || toId == null || amount < 0 || date == null
          || (fromTable.equals(toTable) && fromId.equals(toId))) {
        printUsageAndExit();
        return;
      }
      myBank.transfer(fromTable, fromId, toTable, toId, amount, date);
    } else if (action.equalsIgnoreCase("cancel")) {
      if (id == null || date == null) {
        printUsageAndExit();
        return;
      }
      myBank.cancel(id, date);
    } else if (action.equalsIgnoreCase("getBalance")) {
      if (table == null || id == null) {
        printUsageAndExit();
        return;
      }
      int balance = myBank.getBalance(table, id);
      System.out.println("The balance for " + id + " is " + balance);
    }
    myBank.close();
  }

  private static void printUsageAndExit() {
    System.err.println(
      "MyBankMain -action deposit/withdraw/transfer/cancel/getBalance\n" +
      " [-fromTable table(mysql or postgres) (needed for transfer)]\n" +
      " [-fromId id (needed for transfer)]\n" +
      " [-toTable table(mysql or postgres) (needed for transfer)]\n" +
      " [-toId id (needed for transfer)]\n" +
      " [-table table(mysql or postgres) (needed for deposit, withdraw, getBalance)]\n" +
      " [-id id (needed for deposit, withdraw, cancel, getBalance)]\n" +
      " [-amount number (needed for deposit, withdraw, transfer)]\n" +
      " [-date format(yyyyMMddHHmm) (needed for transfer, cancel)]");
    System.exit(1);
  }
}
