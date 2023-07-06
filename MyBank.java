package sample;

import com.scalar.db.api.DistributedTransaction;
import com.scalar.db.api.DistributedTransactionManager;
import com.scalar.db.api.Get;
import com.scalar.db.api.Put;
import com.scalar.db.api.Result;
import com.scalar.db.exception.transaction.TransactionException;
import com.scalar.db.io.Key;
import com.scalar.db.service.TransactionFactory;
import java.io.IOException;
import java.util.Optional;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.text.ParseException;

public class MyBank {

  private static String NAMESPACE = null;
  private static String FRNAMESPACE = null;
  private static String TONAMESPACE = null;
  private static final int THRESHOLD = 7;

  private final DistributedTransactionManager manager;

  public MyBank(String scalarDBProperties) throws IOException {
    TransactionFactory factory = TransactionFactory.create(scalarDBProperties);
    manager = factory.getTransactionManager();
  }

  public void deposit(String table, String id, int amount) throws TransactionException {
    // Start a transaction
    DistributedTransaction tx = manager.start();

    try {
      // Select table
      if (table.equals("mysql")) {
        NAMESPACE = "mmybank";
      } else if (table.equals("postgres")) {
        NAMESPACE = "pmybank";
      } else {
        throw new RuntimeException("The table name is invalid");
      }

      // Retrieve the current balance for id
      Get get =
        Get.newBuilder()
          .namespace(NAMESPACE)
          .table("accounts")
          .partitionKey(Key.ofText("accountId", id))
          .build();
      Optional<Result> result = tx.get(get);

      // Calculate the balance
      int newBalance = amount;
      if (result.isPresent()) {
        int current = result.get().getInt("balance");
        newBalance += current;
      }

      // Update the balance
      Put put =
        Put.newBuilder()
          .namespace(NAMESPACE)
          .table("accounts")
          .partitionKey(Key.ofText("accountId", id))
          .intValue("balance", newBalance)
          .build();
      tx.put(put);

      // Commit the transaction
      tx.commit();
    } catch (Exception e) {
      tx.abort();
      throw e;
    }
  }

  public void withdraw(String table, String id, int amount) throws TransactionException {
    // Start a transaction
    DistributedTransaction tx = manager.start();

    try {
      // Select table
      if (table.equals("mysql")) {
        NAMESPACE = "mmybank";
      } else if (table.equals("postgres")) {
        NAMESPACE = "pmybank";
      } else {
        throw new RuntimeException("The table name is invalid");
      }

      // Retrieve the current balance for id
      Get get =
        Get.newBuilder()
          .namespace(NAMESPACE)
          .table("accounts")
          .partitionKey(Key.ofText("accountId", id))
          .build();
      Optional<Result> result = tx.get(get);

      // Calculate the balance
      int newBalance = -1;
      if (result.isPresent()) {
        newBalance = result.get().getInt("balance") - amount;
      }
      if (newBalance < 0) {
        throw new RuntimeException("The id or the amount is invalid");
      }

      // Update the balance
      Put put =
        Put.newBuilder()
          .namespace(NAMESPACE)
          .table("accounts")
          .partitionKey(Key.ofText("accountId", id))
          .intValue("balance", newBalance)
          .build();
      tx.put(put);

      // Commit the transaction
      tx.commit();
    } catch (Exception e) {
      tx.abort();
      throw e;
    }
  }

  public void transfer(String fromTable, String fromId, String toTable, String toId, int amount, String date) throws TransactionException {
    // Start a transaction
    DistributedTransaction tx = manager.start();

    try {
      // Select table
      if (fromTable.equals("mysql")) {
        FRNAMESPACE = "mmybank";
      } else if (fromTable.equals("postgres")) {
        FRNAMESPACE = "pmybank";
      } else {
        throw new RuntimeException("The table name is invalid");
      }

      if (toTable.equals("mysql")) {
        TONAMESPACE = "mmybank";
      } else if (toTable.equals("postgres")) {
        TONAMESPACE = "pmybank";
      } else {
        throw new RuntimeException("The table name is invalid");
      }

      // Retrieve the current balances for ids
      Get fromGet =
        Get.newBuilder()
          .namespace(FRNAMESPACE)
          .table("accounts")
          .partitionKey(Key.ofText("accountId", fromId))
          .build();
      Get toGet =
        Get.newBuilder()
          .namespace(TONAMESPACE)
          .table("accounts")
          .partitionKey(Key.ofText("accountId", toId))
          .build();
      Optional<Result> fromResult = tx.get(fromGet);
      Optional<Result> toResult = tx.get(toGet);

      // Calculate the balances
      int newFromBalance = -1;
      int newToBalance = -1;   
      if (fromResult.isPresent() && toResult.isPresent()) {
        newFromBalance = fromResult.get().getInt("balance") - amount;
        newToBalance = toResult.get().getInt("balance") + amount;
      }
      if (newFromBalance < 0 || newToBalance < 0) {
        throw new RuntimeException("The id or the amount is invalid");
      }

      // Update the balances
      Put fromPut =
        Put.newBuilder()
          .namespace(FRNAMESPACE)
          .table("accounts")
          .partitionKey(Key.ofText("accountId", fromId))
          .intValue("balance", newFromBalance)
          .build();
      Put toPut =
        Put.newBuilder()
          .namespace(TONAMESPACE)
          .table("accounts")
          .partitionKey(Key.ofText("accountId", toId))
          .intValue("balance", newToBalance)
          .build();
      tx.put(fromPut);
      tx.put(toPut);

      try {
        // Confirm date format (maybe incomplete)
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
        Date dDate = format.parse(date);
        Calendar cDate = Calendar.getInstance();
        cDate.setTime(dDate);
      } catch (ParseException e) {
        throw new RuntimeException("The date format is invalid");
      }

      // This id used as transaction id (unique)
      String id = fromId + '/' + toId + '/' + date;

      // Update the transaction table      
      if (FRNAMESPACE == TONAMESPACE) {
        Put trPut =
          Put.newBuilder()
            .namespace(FRNAMESPACE)
            .table("transactions")
            .partitionKey(Key.ofText("transactionId", id))
            .textValue("fromTable", fromTable)
            .textValue("fromId", fromId)
            .textValue("toTable", toTable)
            .textValue("toId", toId)
            .intValue("amount", amount)
            .textValue("date", date)
            .intValue("flag", 1)
            .build();
        tx.put(trPut);
      } else {
        Put fromtrPut =
          Put.newBuilder()
            .namespace(FRNAMESPACE)
            .table("transactions")
            .partitionKey(Key.ofText("transactionId", id))
            .textValue("fromTable", fromTable)
            .textValue("fromId", fromId)
            .textValue("toTable", toTable)
            .textValue("toId", toId)
            .intValue("amount", amount)
            .textValue("date", date)
            .intValue("flag", 1)
            .build();
        tx.put(fromtrPut);
        Put totrPut =
          Put.newBuilder()
            .namespace(TONAMESPACE)
            .table("transactions")
            .partitionKey(Key.ofText("transactionId", id))
            .textValue("fromTable", fromTable)
            .textValue("fromId", fromId)
            .textValue("toTable", toTable)
            .textValue("toId", toId)
            .intValue("amount", amount)
            .textValue("date", date)
            .intValue("flag", 1)
            .build();
        tx.put(totrPut);
      }

      // Commit the transaction
      tx.commit();
    } catch (Exception e) {
      tx.abort();
      throw e;
    }
  }

  public void cancel(String id, String date) throws TransactionException {
    // Start a transaction
    DistributedTransaction tx = manager.start();

    try {
      // Retrieve the ids, amount, date, and flag for transaction id
      Get myGet = 
        Get.newBuilder()
          .namespace("mmybank")
          .table("transactions")
          .partitionKey(Key.ofText("transactionId", id))
          .build();
      Optional<Result> myResult = tx.get(myGet);
      Get poGet = 
        Get.newBuilder()
          .namespace("pmybank")
          .table("transactions")
          .partitionKey(Key.ofText("transactionId", id))
          .build();
      Optional<Result> poResult = tx.get(poGet);

      int myflag = 0;
      int poflag = 0;

      if (myResult.isPresent()) {
        myflag = 1;
      }
      if (poResult.isPresent()) {
        poflag = 1;
      }
      if (myflag != 1 && poflag != 1) {
        throw new RuntimeException("The id is invalid");
      }

      if (myflag == 1) {
        String fromTable = myResult.get().getText("toTable");
        String fromId = myResult.get().getText("toId");
        String toTable = myResult.get().getText("fromTable");
        String toId = myResult.get().getText("fromId");
        int amount = myResult.get().getInt("amount");
        String trDate = myResult.get().getText("date");
        int flag = myResult.get().getInt("flag");
        if (flag == 1) {
          // set flag as unavailable
          Put trPut =
            Put.newBuilder()
              .namespace("mmybank")
              .table("transactions")
              .partitionKey(Key.ofText("transactionId", id))
              .intValue("flag", 0)
              .build();
          tx.put(trPut);

          try {
            // Change to type calendar (maybe incomplete)
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
            Date dDate = format.parse(date);
            Date dtrDate = format.parse(trDate);
            Calendar cDate = Calendar.getInstance();
            Calendar ctrDate = Calendar.getInstance();
            cDate.setTime(dDate);
            ctrDate.setTime(dtrDate);
            ctrDate.add(Calendar.DAY_OF_MONTH, THRESHOLD);

            // The cancel must be within THRESHOLD days (in this case, 7 days)
            if (cDate.compareTo(ctrDate) > 0) {
              if (poflag == 1) {
                // set flag as unavailable
                Put potrPut =
                  Put.newBuilder()
                    .namespace("pmybank")
                    .table("transactions")
                    .partitionKey(Key.ofText("transactionId", id))
                    .intValue("flag", 0)
                    .build();
                tx.put(potrPut);
              }
              // Commit the transaction (only set flag as unavailable)
              tx.commit();
              System.err.println("The cancel must be within 7 days");
              System.exit(1);
            }
          } catch (ParseException e) {
            throw new RuntimeException("The date format is invalid");
          }
        } else {
          throw new RuntimeException("The transaction cannot be canceled (already canceled or expired)");
        }

        // Select table
        if (fromTable.equals("mysql")) {
          FRNAMESPACE = "mmybank";
        } else if (fromTable.equals("postgres")) {
          FRNAMESPACE = "pmybank";
        } else {
          throw new RuntimeException("The table name is invalid");
        }

        if (toTable.equals("mysql")) {
          TONAMESPACE = "mmybank";
        } else if (toTable.equals("postgres")) {
          TONAMESPACE = "pmybank";
        } else {
          throw new RuntimeException("The table name is invalid");
        }

        // Retrieve the current balances for ids
        Get fromGet =
          Get.newBuilder()
            .namespace(FRNAMESPACE)
            .table("accounts")
            .partitionKey(Key.ofText("accountId", fromId))
            .build();
        Get toGet =
          Get.newBuilder()
            .namespace(TONAMESPACE)
            .table("accounts")
            .partitionKey(Key.ofText("accountId", toId))
            .build();
        Optional<Result> fromResult = tx.get(fromGet);
        Optional<Result> toResult = tx.get(toGet);

        // Calculate the balances
        int newFromBalance = -1;
        int newToBalance = -1;   
        if (fromResult.isPresent() && toResult.isPresent()) {
          newFromBalance = fromResult.get().getInt("balance") - amount;
          newToBalance = toResult.get().getInt("balance") + amount;
        }
        if (newFromBalance < 0 || newToBalance < 0) {
          throw new RuntimeException("The transaction cannot be canceled (account does not have enough balance)");
        }

        // Update the balances
        Put fromPut =
          Put.newBuilder()
            .namespace(FRNAMESPACE)
            .table("accounts")
            .partitionKey(Key.ofText("accountId", fromId))
            .intValue("balance", newFromBalance)
            .build();
        Put toPut =
          Put.newBuilder()
            .namespace(TONAMESPACE)
            .table("accounts")
            .partitionKey(Key.ofText("accountId", toId))
            .intValue("balance", newToBalance)
            .build();
        tx.put(fromPut);
        tx.put(toPut);
      }

      if (poflag == 1) {
        String fromTable = poResult.get().getText("toTable");
        String fromId = poResult.get().getText("toId");
        String toTable = poResult.get().getText("fromTable");
        String toId = poResult.get().getText("fromId");
        int amount = poResult.get().getInt("amount");
        String trDate = poResult.get().getText("date");
        int flag = poResult.get().getInt("flag");
        if (flag == 1) {
          // set flag as unavailable
          Put trPut =
            Put.newBuilder()
              .namespace("pmybank")
              .table("transactions")
              .partitionKey(Key.ofText("transactionId", id))
              .intValue("flag", 0)
              .build();
          tx.put(trPut);

          try {
            // Change to type calendar (maybe incomplete)
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
            Date dDate = format.parse(date);
            Date dtrDate = format.parse(trDate);
            Calendar cDate = Calendar.getInstance();
            Calendar ctrDate = Calendar.getInstance();
            cDate.setTime(dDate);
            ctrDate.setTime(dtrDate);
            ctrDate.add(Calendar.DAY_OF_MONTH, THRESHOLD);

            // The cancel must be within THRESHOLD days (in this case, 7 days)
            if (cDate.compareTo(ctrDate) > 0) {
              // Commit the transaction (only set flag as unavailable)
              tx.commit();
              System.err.println("The cancel must be within 7 days");
              System.exit(1);
            }
          } catch (ParseException e) {
            throw new RuntimeException("The date format is invalid");
          }
        } else {
          throw new RuntimeException("The transaction cannot be canceled (already canceled or expired)");
        }

        if (myflag != 1) {
          // Select table
          if (fromTable.equals("mysql")) {
            FRNAMESPACE = "mmybank";
          } else if (fromTable.equals("postgres")) {
            FRNAMESPACE = "pmybank";
          } else {
            throw new RuntimeException("The table name is invalid");
          }

          if (toTable.equals("mysql")) {
            TONAMESPACE = "mmybank";
          } else if (toTable.equals("postgres")) {
            TONAMESPACE = "pmybank";
          } else {
            throw new RuntimeException("The table name is invalid");
          }

          // Retrieve the current balances for ids
          Get fromGet =
            Get.newBuilder()
              .namespace(FRNAMESPACE)
              .table("accounts")
              .partitionKey(Key.ofText("accountId", fromId))
              .build();
          Get toGet =
            Get.newBuilder()
              .namespace(TONAMESPACE)
              .table("accounts")
              .partitionKey(Key.ofText("accountId", toId))
              .build();
          Optional<Result> fromResult = tx.get(fromGet);
          Optional<Result> toResult = tx.get(toGet);

          // Calculate the balances
          int newFromBalance = -1;
          int newToBalance = -1;   
          if (fromResult.isPresent() && toResult.isPresent()) {
            newFromBalance = fromResult.get().getInt("balance") - amount;
            newToBalance = toResult.get().getInt("balance") + amount;
          }
          if (newFromBalance < 0 || newToBalance < 0) {
            throw new RuntimeException("The transaction cannot be canceled (account does not have enough balance)");
          }

          // Update the balances
          Put fromPut =
            Put.newBuilder()
              .namespace(FRNAMESPACE)
              .table("accounts")
              .partitionKey(Key.ofText("accountId", fromId))
              .intValue("balance", newFromBalance)
              .build();
          Put toPut =
            Put.newBuilder()
              .namespace(TONAMESPACE)
              .table("accounts")
              .partitionKey(Key.ofText("accountId", toId))
              .intValue("balance", newToBalance)
              .build();
          tx.put(fromPut);
          tx.put(toPut);
        }
      }

      // Commit the transaction
      tx.commit();
    } catch (Exception e) {
      tx.abort();
      throw e;
    }
  }

  public int getBalance(String table, String id) throws TransactionException {
    // Start a transaction
    DistributedTransaction tx = manager.start();

    try {
      // Select table
      if (table.equals("mysql")) {
        NAMESPACE = "mmybank";
      } else if (table.equals("postgres")) {
        NAMESPACE = "pmybank";
      } else {
        throw new RuntimeException("The table name is invalid");
      }

      // Retrieve the current balances for id
      Get get =
        Get.newBuilder()
          .namespace(NAMESPACE)
          .table("accounts")
          .partitionKey(Key.ofText("accountId", id))
          .build();
      Optional<Result> result = tx.get(get);

      int balance = -1;
      if (result.isPresent()) {
        balance = result.get().getInt("balance");
      } else {
        throw new RuntimeException("The id is invalid");
      }

      // Commit the transaction
      tx.commit();

      return balance;
    } catch (Exception e) {
      tx.abort();
      throw e;
    }
  }

  public void close() {
    manager.close();
  }
}
