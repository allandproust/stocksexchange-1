package com.stocks.model.dao.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.StringTokenizer;

import com.stocks.model.BuySellStock;
import com.stocks.model.DBConnection;
import com.stocks.model.Stock;
import com.stocks.model.dao.StocksDao;

public class StocksDaoImpl implements StocksDao {

	public List<Stock> getHighFrequencyStocks(int lowRange, int highRange) {
		Connection conn = null;
		List<Stock> stocks = new ArrayList<Stock>();
		try {
			DBConnection db = new DBConnection();
			conn = db.getConnection();
				  
			String sql = "select stock_symbol, count(*) as frequency, sum(stock_value) total_value "
				+ "from most_active_stocks "
				+ "where closing_date IN ( "
				+ "select a.closedate "
				+ "from ( "
				+  "SELECT * "
				+  "FROM (SELECT ROWNUM rnum "
				+  "          ,b.* "
				+        "FROM ( "
				+          "select distinct closing_date as closedate " 
				+          "from most_active_stocks " 
				+          "order by closing_date desc "
				+       ") b "
				+   ") "
				+   "WHERE rnum BETWEEN ? AND ? "				  
				+ ") a "
				+ ") "
				+ "group by stock_symbol "
				+ "order by frequency desc, total_value desc";
				PreparedStatement prest = conn.prepareStatement(sql);
				prest.setInt(1, lowRange);
				prest.setInt(2, highRange);
				ResultSet rs = prest.executeQuery();
				while (rs.next()){
					Stock stock = new Stock();
					stock.setStockSymbol(rs.getString(1));
					stock.setFrequency(rs.getInt(2));
					double totalValue = rs.getDouble(3);
					stock.setTotalStockValue(new BigDecimal(totalValue).setScale(2, RoundingMode.CEILING));
					stocks.add(stock);
				}
				rs.close();
				prest.close();
				conn.close();
			  } catch (Exception e) {
				  e.printStackTrace();
			  }
			  return stocks;
	}
	
	public Map<String,Object> getPSEHistoricalData(String symbol, int lowRange, int highRange) {
		  Connection conn = null;
		  List<Stock> stocks = new ArrayList<Stock>();
		  Map<String,Object> historicalMap = new HashMap<String,Object>();
		  try {
			  DBConnection db = new DBConnection();
			  conn = db.getConnection();

			  String sql = "select max(closing_date) "
				  + "from most_active_stocks "
				  + "where closing_date IN ( "
				  + "select a.closedate "
				  + "from ( "
				  +  "SELECT * "
				  +  "FROM (SELECT ROWNUM rnum "
				  +  "          ,b.* "
				  +        "FROM ( "
				  +          "select distinct closing_date as closedate " 
				  +          "from most_active_stocks " 
				  +          "order by closing_date desc "
				  +       ") b "
				  +   ") "
				  +   "WHERE rnum BETWEEN ? AND ? "
				  + ") a "
				  + ") "
				  + "and stock_symbol = ? ";

			    PreparedStatement prest = conn.prepareStatement(sql);
			    prest.setInt(1, lowRange);
				prest.setInt(2, highRange);
			    prest.setString(3, symbol);
			    ResultSet rs = prest.executeQuery();
			    while (rs.next()){
					historicalMap.put("latestMostActive",rs.getDate(1));
			    }
			    rs.close();
			    prest.close();


			  String sql2 = "select stock_symbol, last_trading_price, stock_value, closing_date "
				  + "from most_active_stocks "
				  + "where closing_date IN ( "
				  + "select a.closedate "
				  + "from ( "
				  +  "SELECT * "
				  +  "FROM (SELECT ROWNUM rnum "
				  +  "          ,b.* "
				  +        "FROM ( "
				  +          "select distinct closing_date as closedate " 
				  +          "from most_active_stocks " 
				  +          "order by closing_date desc "
				  +       ") b "
				  +   ") "
				  +   "WHERE rnum BETWEEN ? AND ? "				  
				  + ") a "
				  + ") "
				  + "and stock_symbol = ? "
				  + "order by last_trading_price asc";

			    PreparedStatement prest2 = conn.prepareStatement(sql2);
			    prest2.setInt(1, lowRange);
				prest2.setInt(2, highRange);
			    prest2.setString(3, symbol);
			    ResultSet rs2 = prest2.executeQuery();
			    while (rs2.next()){
					Stock stock = new Stock();
			    	String stockSymbol = rs2.getString(1);
					double lastPrice = rs2.getDouble(2);
					double stockValue = rs2.getDouble(3);
					Date closingDate = rs2.getDate(4);
					stock.setStockSymbol(stockSymbol);
					stock.setLastPrice(lastPrice);
					stock.setStockValue(new BigDecimal(stockValue).setScale(2, RoundingMode.CEILING));
					stock.setClosingDate(closingDate);
					stocks.add(stock);
			    }
			    rs2.close();
			    prest2.close();
				historicalMap.put("stocksList",stocks);
			  conn.close();
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
		  return historicalMap;
	}
	
	public int importStocksData(String symbol, double lastPrice, String tradingDate){
		Connection conn = null;
		try {
			DBConnection db = new DBConnection();
			conn = db.getConnection();
			String sql = "INSERT INTO most_active_stocks(id, stock_symbol,last_trading_price,closing_date) " 
			  +"VALUES(most_active_stocks_seq.NEXTVAL,?,?,?)";
			PreparedStatement prest = conn.prepareStatement(sql);
			prest.setString(1, symbol);
			prest.setDouble(2, lastPrice);
			prest.setDate(3,java.sql.Date.valueOf(tradingDate));
			prest.executeUpdate();
			prest.close();
			conn.close();
		} catch(Exception e) {
            System.out.println("Exception while inserting most active stock data: " + e.getMessage());
            return 1;
        }
		return 0;
	}

	public int importLiveData(List<Map<String,String>> stocksList, int lowRange, int highRange){
		Connection conn = null;
		try
        {
			DBConnection db = new DBConnection();
			conn = db.getConnection();
			String tradingDate = "";
			for(Map<String,String> sds : stocksList){
				if(sds.get("lastTradedPrice").equals("DATE")){
					String[] nextTokenArr = sds.get("securityAlias").substring(0, 10).split("/");
					if(nextTokenArr.length > 1){
						tradingDate = nextTokenArr[2] + "-" + nextTokenArr[0] +"-" + nextTokenArr[1];
					}
		        } else {
		        	String sql = "INSERT INTO all_active_stocks(id, stock_symbol,last_trading_price, stock_value, stock_amount, closing_date) " 
	        				  +"VALUES(ALL_ACTIVE_STOCKS_SEQ.NEXTVAL,?,?,?,?,?)";
		        	PreparedStatement prest = conn.prepareStatement(sql);
		        	prest.setString(1, sds.get("securitySymbol"));
		        	prest.setDouble(2, Double.parseDouble(sds.get("lastTradedPrice")));
		        	prest.setDouble(3, Double.parseDouble(sds.get("totalVolume")));
		        	prest.setBigDecimal(4, new BigDecimal(Double.parseDouble(sds.get("totalVolume")) * Double.parseDouble(sds.get("lastTradedPrice"))).setScale(2, RoundingMode.CEILING));
		        	prest.setDate(5,java.sql.Date.valueOf(tradingDate));
		        	prest.executeUpdate();
		        	prest.close();
		        }
			}
			
			// get the top 50 most active stocks from PSE and populate the most_active_stocks table
			String sql2 = "INSERT INTO MOST_ACTIVE_STOCKS (ID, STOCK_SYMBOL, LAST_TRADING_PRICE, STOCK_VALUE, CLOSING_DATE) " +
			    "SELECT most_active_stocks_seq.NEXTVAL, a.STOCK_SYMBOL,a.LAST_TRADING_PRICE, a.STOCK_AMOUNT, a.CLOSING_DATE " +
				"FROM (SELECT ROWNUM rnum, b.* " +
			    "      FROM (select stock_symbol, last_trading_price, stock_amount, closing_date " +
				"            from all_active_stocks " +
				"            WHERE closing_date = ? " +
			    "            order by stock_amount desc " +
				"      ) b " +
				") a " +
			    "WHERE a.rnum BETWEEN ? AND ?";

             PreparedStatement prest2 = conn.prepareStatement(sql2);
             prest2.setDate(1,java.sql.Date.valueOf(tradingDate));
             prest2.setInt(2, lowRange);
			 prest2.setInt(3, highRange);
             prest2.executeUpdate();
             prest2.close();
			 conn.close();
		}
        catch(Exception e)
        {
                System.out.println("Exception while inserting stock records: " + e);
                return 1;
        }
		return 0;
	}
	
	public List<Stock> getAllPSEStocks() {
		  Connection conn = null;
		  List<Stock> stocks = new ArrayList<Stock>();
		  try {
			  DBConnection db = new DBConnection();
			  conn = db.getConnection();
			  
			  String sql = "select stock_symbol, count(*) as frequency "
				  + "from most_active_stocks "
				  + "group by stock_symbol "
				  + "order by frequency desc";
			    PreparedStatement prest = conn.prepareStatement(sql);
			    ResultSet rs = prest.executeQuery();
			    while (rs.next()){
					Stock stock = new Stock();
			    	String stockSymbol = rs.getString(1);
					int frequency = rs.getInt(2);
					stock.setStockSymbol(stockSymbol);
					stock.setFrequency(frequency);					
					stocks.add(stock);
			    }
			    rs.close();
			    prest.close();
			  conn.close();
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
		  return stocks;
	}

	public int removeStocksData(String tradingDate){
		Connection conn = null;
		try
        {
			DBConnection db = new DBConnection();
			conn = db.getConnection();
			
			String sql = "DELETE FROM all_active_stocks WHERE closing_date = ? ";
			PreparedStatement prest = conn.prepareStatement(sql);
			prest.setDate(1,java.sql.Date.valueOf(tradingDate));
			prest.executeUpdate();
			prest.close();

			String sql2 = "DELETE FROM most_active_stocks WHERE closing_date = ? ";
			PreparedStatement prest2 = conn.prepareStatement(sql2);
			prest2.setDate(1,java.sql.Date.valueOf(tradingDate));
			prest2.executeUpdate();
			prest2.close();

			conn.close();
		} catch(Exception e) {
                System.out.println("Exception while removing stock records: " + e);
                return 1;
        }
		return 0;
	}

	public Map<String,Object> getHistoricalData(int lowRange, int highRange) {
		  Connection conn = null;
		  List<Stock> stocks = null;
		  Map<String,Object> historicalMap = new HashMap<String,Object>();
		  try {
			  DBConnection db = new DBConnection();
			  conn = db.getConnection();

			  String sql2 = "select stock_symbol, last_trading_price, stock_value, closing_date "
				  + "from most_active_stocks "
				  + "where closing_date IN ( "
				  + "select a.closedate "
				  + "from ( "
				  +  "SELECT * "
				  +  "FROM (SELECT ROWNUM rnum "
				  +  "          ,b.* "
				  +        "FROM ( "
				  +          "select distinct closing_date as closedate " 
				  +          "from most_active_stocks " 
				  +          "order by closing_date desc "
				  +       ") b "
				  +   ") "
				  +   "WHERE rnum BETWEEN ? AND ? "				  
				  + ") a "
				  + ") "
				  + "order by stock_symbol asc, last_trading_price asc";

			    PreparedStatement prest2 = conn.prepareStatement(sql2);
			    prest2.setInt(1, lowRange);
				prest2.setInt(2, highRange);
			    ResultSet rs2 = prest2.executeQuery();
			    String prevStockSymbol = null;
			    Date latestMostActive = null;
			    while (rs2.next()){
					Stock stock = new Stock();
			    	String stockSymbol = rs2.getString(1);
					double lastPrice = rs2.getDouble(2);
					double stockValue = rs2.getDouble(3);
					Date closingDate = rs2.getDate(4);
					stock.setStockSymbol(stockSymbol);
					stock.setLastPrice(lastPrice);
					stock.setStockValue(new BigDecimal(stockValue).setScale(2, RoundingMode.CEILING));
					stock.setClosingDate(closingDate);
					
					if(prevStockSymbol == null){
						prevStockSymbol = stockSymbol;
						latestMostActive = closingDate;
						stocks = new ArrayList<Stock>();
					}
			    	if(prevStockSymbol!=null && !prevStockSymbol.equals(stockSymbol)){
			    		historicalMap.put(prevStockSymbol,stocks);
			    		historicalMap.put(prevStockSymbol+"latestMostActive",latestMostActive);
			    		stocks = new ArrayList<Stock>();
			    		prevStockSymbol = stockSymbol;
			    		latestMostActive = closingDate;
			    	}
			    	
			    	if(closingDate.getTime() > latestMostActive.getTime()){
			    		latestMostActive = closingDate;
			    	}
			    	
			    	stocks.add(stock);
			    	
			    }
			    historicalMap.put(prevStockSymbol,stocks);
			    historicalMap.put(prevStockSymbol+"latestMostActive",latestMostActive);
			    
			    rs2.close();
			    prest2.close();
			  conn.close();
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
		  return historicalMap;
	}

	public Map<String,BigDecimal> getBuySellStocks() {
		  Connection conn = null;
		  Map<String,BigDecimal> buySellStocks = new HashMap<String,BigDecimal>();
		  try {
			  DBConnection db = new DBConnection();
			  conn = db.getConnection();

			  String sql = "SELECT stock_symbol, buy_price, sell_price "
				  + "FROM buy_sell_stocks "
				  + "WHERE is_enabled = ? "
				  + "AND is_deleted = ? ";
			  
			  PreparedStatement prest = conn.prepareStatement(sql);
			  prest.setString(1, "Y");
			  prest.setString(2, "N");
			  ResultSet rs = prest.executeQuery();
			  while (rs.next()){
				  String stockSymbol = rs.getString(1);
				  double buyPrice = rs.getDouble(2);
				  double sellPrice = rs.getDouble(3);
				  buySellStocks.put(stockSymbol+"_SELL",new BigDecimal(sellPrice));
				  buySellStocks.put(stockSymbol+"_BUY",new BigDecimal(buyPrice));
			  }
			  rs.close();
			  prest.close();
			  conn.close();
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
		  return buySellStocks;
	}
	
	public List<BuySellStock> getBuySellStocks(String isEnabled, String isDeleted){
		  Connection conn = null;
		  List<BuySellStock> priceAlerts = new ArrayList<BuySellStock>();
		  try {
			  DBConnection db = new DBConnection();
			  conn = db.getConnection();

			  String sql = "SELECT id, stock_symbol, buy_price, sell_price, created_date "
				  + "FROM buy_sell_stocks "
				  + "WHERE is_enabled = ? "
				  + "AND is_deleted = ? "
				  + "ORDER BY stock_symbol";
			  
			  PreparedStatement prest = conn.prepareStatement(sql);
			  prest.setString(1, isEnabled);
			  prest.setString(2, isDeleted);
			  ResultSet rs = prest.executeQuery();
			  while (rs.next()){
				  BuySellStock buySellStock = new BuySellStock();
				  buySellStock.setId(rs.getLong(1));
				  buySellStock.setStockSymbol(rs.getString(2));
				  buySellStock.setBuyPrice(rs.getDouble(3));
				  buySellStock.setSellPrice(rs.getDouble(4));
				  buySellStock.setCreateDate(rs.getDate(5));
				  priceAlerts.add(buySellStock);
			  }
			  rs.close();
			  prest.close();
			  conn.close();
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
		return priceAlerts;
	}
	
	public void updateBuySellStocks(long id, String isEnabled, String isDeleted){
		Connection conn = null;
		try
        {
			DBConnection db = new DBConnection();
			conn = db.getConnection();
			
			String sql = "UPDATE buy_sell_stocks "
					+ "SET is_enabled = ?, is_deleted = ?, modified_date = SYSDATE "
					+ "WHERE id = ? ";
			PreparedStatement prest = conn.prepareStatement(sql);
			prest.setString(1, isEnabled);
			prest.setString(2, isDeleted);
			prest.setLong(3, id);
			prest.executeUpdate();
			prest.close();

			conn.close();
		}
        catch(Exception e)
        {
                System.out.println("Exception while updating buy and sell records: " + e.getMessage());
        }
	}
	
	public void addBuySellStock(String stockSymbol, double buyPrice, double sellPrice){
		Connection conn = null;
		try
        {
			DBConnection db = new DBConnection();
			conn = db.getConnection();
			
			String sql = "INSERT INTO buy_sell_stocks(id, stock_symbol, buy_price, sell_price, created_date) "
					+ "VALUES(BUY_SELL_STOCKS_SEQ.NEXTVAL, ?, ?, ?, SYSDATE) ";
			PreparedStatement prest = conn.prepareStatement(sql);
			prest.setString(1, stockSymbol);
			prest.setDouble(2, buyPrice);
			prest.setDouble(3, sellPrice);
			prest.executeUpdate();
			prest.close();

			conn.close();
		}
        catch(Exception e)
        {
                System.out.println("Exception while updating buy and sell records: " + e.getMessage());
        }
	}
}
