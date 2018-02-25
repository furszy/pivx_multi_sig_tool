# PIVX MultiSig Tool

Fast tool to create and redeem multi-sig addresses.


### Create a multiSig address

java -jar pivx-multi-sig.jar -gen -pubKeys pubHex1,pubHex2...

Example:

java -jar pivx-multi-sig.jar -gen -pubKeys 023910b54c9ee1ab2570efc5ef25b93139cd81c25780b35ea2b9a088b5d3557ae7,023910b54c9ee1ab2570efc5ef25b93139cd81c25780b35ea2b9a088b5d3557ae7


### Create a transaction and send the amount signing with the first key

java -jar pivx_multi-sig.jar -createTx -key privHex,pubHex -redeemScript redeemScriptHex -redeemOutputHex outputHex -redeemOutputIndex outputIndex -redeemOutputTxHash outputTxHash -toAddress address -amount amount


### Sign a previously created transaction with the second key

java -jar pivx_multi-sig.jar -sign -key privHex,pubHex -rawTxHex rawTxHex
