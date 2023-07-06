
------------------------------Note------------------------------

---accountId is unique (in a table).
	-> There can be user1 in mysql and postgres respectively.

---transactionId is unique.
	-> fromId, toId, and date are not the same at the same time.
	-> especially date is assumed not identical.
	-> the transactionId is represented by a concatenation of fromId, toId, and date.

---the name of argument "table".
	-> In the real world, "table" name is regarded as "bank" name.

---cannot deal with SimpleDateFormat well.
	-> date should be appropriate format.
	-> If date is not expressed in the proper format,
		unexpected behavior may occur.

---any errors which developer were unaware may exist.
	-> throw errors for inappropriate operations as much as possible.

---why date is required?
	-> to make it easy to check the debug. 





------------------------------Usage------------------------------



---------------environment---------------

java -jar scalardb-schema-loader-3.9.0.jar --config scalardb.properties
--schema-file mybank.json --coordinator



---------------argument---------------

fromTable	-> "mysql" or "postgres"
fromId		-> accountId
toTable		-> "mysql" or "postgres"
toId		-> accountId
table		-> "mysql" or "postgres"
id		-> accountId (deposit, withdraw, getBalance)
		-> transactionId (cancel)
amount		-> positive number
date		-> formatted String (yyyyMMddHHmm)
		-> yyyy(year), MM(month), dd(day), HH(24hour), mm(minute)



---------------function---------------


----------deposit----------

-----description-----
Create an account with accountId and initialize its balance with amount,
or if accountId account already exists, add amount to the balance.

-----usage-----
gradlew run --args="-action deposit -table table -id id -amount number"


----------withdraw----------

-----description-----
Withdraw amount from accountId account.

-----usage-----
gradlew run --args="-action withdraw -table table -id id -amount number"


----------transfer----------

-----description-----
Transfer amount from fromId account to toId account.

-----usage-----
gradlew run --args="-action transfer -fromTable table -fromId id
		-toTable table -toId id -amount number -date format"


----------cancel----------

-----description-----
If the conditions are met, cancel the transactionId transaction.

-----usage-----
gradlew run --args="-action cancel -id id -date format"


----------getBalance----------

-----description-----
Get information abount balance of accountId account.

-----usage-----
gradlew run --args="-action getBalance -table table -id id"