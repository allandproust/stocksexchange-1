package com.stocks.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.StringTokenizer;

import com.stocks.model.DBConnection;

public class SimpleSelect {

	public List<Stock> getHighFrequencyStocks() {
		  Connection conn = null;
		  List<Stock> stocks = new ArrayList<Stock>();
		  try {
			  DBConnection db = new DBConnection();
			  conn = db.getConnection();
			  System.out.println("Connected to the database");
			  
			  String sql = "select stock_symbol, count(*) as frequency "
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
				  +   "WHERE rnum BETWEEN 1 AND 10 "				  
				  + ") a "
				  + ") "
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
			  conn.close();
			  System.out.println("Disconnected from database");
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
		  return stocks;
	}
	
	public Map<String,Object> getHistoricalData(String symbol) {
		  Connection conn = null;
		  List<Stock> stocks = new ArrayList<Stock>();
		  Map<String,Object> historicalMap = new HashMap<String,Object>();
		  try {
			  DBConnection db = new DBConnection();
			  conn = db.getConnection();
			  System.out.println("Connected to the database");

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
				  +   "WHERE rnum BETWEEN 1 AND 10 "
				  + ") a "
				  + ") "
				  + "and stock_symbol = '"+ symbol + "'";

			    PreparedStatement prest = conn.prepareStatement(sql);
			    ResultSet rs = prest.executeQuery();
			    while (rs.next()){
					historicalMap.put("latestMostActive",rs.getDate(1));
			    }


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
				  +   "WHERE rnum BETWEEN 1 AND 10 "				  
				  + ") a "
				  + ") "
				  + "and stock_symbol = '"+ symbol + "' "
				  + "order by last_trading_price asc";

			    PreparedStatement prest2 = conn.prepareStatement(sql2);
			    ResultSet rs2 = prest2.executeQuery();
			    while (rs2.next()){
					Stock stock = new Stock();
			    	String stockSymbol = rs2.getString(1);
					double lastPrice = rs2.getDouble(2);
					long stockValue = rs2.getLong(3);
					Date closingDate = rs2.getDate(4);
					stock.setStockSymbol(stockSymbol);
					stock.setLastPrice(lastPrice);
					stock.setStockValue(stockValue);
					stock.setClosingDate(closingDate);
					stocks.add(stock);
			    }
				historicalMap.put("stocksList",stocks);
			  conn.close();
			  System.out.println("Disconnected from database");
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
		  return historicalMap;
	}

	public StockScore getStockScore(String symbol, int numShare, double costPerShare, double low, double high){
		StockScore score = new StockScore();
		score.setSymbol(symbol);
		score.setNumShare(numShare);
		score.setCostPerShare(costPerShare);
		score.setLow(low);
		score.setHigh(high);
		return score.computeMe();
		
	}
	
	public Map<String,Object> importData(){
		Map<String,Object> resultsMap = new HashMap<String,Object>();
		Connection conn = null;
        try
        {
               
			  DBConnection db = new DBConnection();
			  conn = db.getConnection();
			  System.out.println("Connected to the database");		  
			  File directory = new File("C:/stocksdatafeed/csv/in");
			  File[] myarray;  
			  myarray=directory.listFiles();
			  for (int j = 0; j < myarray.length; j++)
			  {
			         File path=myarray[j];
			         FileReader fr = new FileReader(path);
			         BufferedReader br = new BufferedReader(fr);
			         String strLine = "";
			         StringTokenizer st = null;
			         int lineNumber = 0, tokenNumber = 0;
               
			         //read comma separated file line by line
			         while( (strLine = br.readLine()) != null)
			         {
                        lineNumber++;
                       
                        if(lineNumber == 1)
                        	continue;
                        //break comma separated line using ","
                        st = new StringTokenizer(strLine, ",");
                       
                        String symbol = null;
                        double lastPrice = 0.0;
                        long stockValue = 0;
                        String tradingDate = null;
                        while(st.hasMoreTokens())
                        {
                                //display csv values
                                tokenNumber++;
                                String nextToken = st.nextToken();
                                if(tokenNumber == 1)
                                	continue;
                                
                                if(tokenNumber == 5){
                                	String[] nextTokenArr = nextToken.split("/");
                                	if(nextTokenArr.length > 1){
                                		nextToken = nextTokenArr[2] + "-" + nextTokenArr[0] +"-" + nextTokenArr[1];
                                		tradingDate = nextToken;
                                	}
                                }
                                
                                if(tokenNumber == 2){
                                	symbol = nextToken;
                                } else if(tokenNumber == 3){
                                	lastPrice = Double.parseDouble(nextToken);
                                } else if(tokenNumber == 4){
                                	stockValue = Long.parseLong(nextToken);
                                }
                        }
                       
                        //reset token number
                        tokenNumber = 0;
                        
          			  	String sql = "INSERT INTO most_active_stocks(id, stock_symbol,last_trading_price,closing_date) " 
        				  +"VALUES(most_active_stocks_seq.NEXTVAL,?,?,?)";
          			  	PreparedStatement prest = conn.prepareStatement(sql);
          			  	prest.setString(1, symbol);
          			  	prest.setDouble(2, lastPrice);
          			  	prest.setDate(3,java.sql.Date.valueOf(tradingDate));
          			  	int count = prest.executeUpdate();
          			  	System.out.println(count + "row(s) affected");
			         }
			         br.close(); //release the resources
					 break;//we want only to process the first file.
			  }

			  //move the read files to archive directory
			  if(myarray.length == 0){
				resultsMap.put("message","No Data to import. Data File not found.");
			  }

			  for (int j = 0; j < myarray.length; j++)
			  {
			    File file=myarray[j];

			  	// Destination directory
			  	File dir = new File("C:/stocksdatafeed/csv/archive");

			  	// Move file to new directory
			  	boolean success = file.renameTo(new File(dir, file.getName()));
			  	if (!success) {
			  		resultsMap.put("message",file.getName() + " was not imported successfully!");
			  	} else {
					resultsMap.put("message",file.getName() + " was imported successfully!");
				}
				
				break;//process only single file
			  }

  			  conn.close();
			  System.out.println("Disconnected from database");
               
               
        }
        catch(Exception e)
        {
                System.out.println("Exception while reading csv file: " + e);                  
        } finally{
			return resultsMap;
        }		
	}

	public int importData(List<Map<String,String>> stocksList){
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
			    "WHERE a.rnum BETWEEN 1 AND 50";

             PreparedStatement prest2 = conn.prepareStatement(sql2);
             prest2.setDate(1,java.sql.Date.valueOf(tradingDate));
             prest2.executeUpdate();
			 conn.close();
		}
        catch(Exception e)
        {
                System.out.println("Exception while inserting stock records: " + e);
                return 1;
        }
		return 0;
	}
	
	public List<Stock> getAllStocks() {
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
			  conn.close();
			  System.out.println("Disconnected from database");
		  } catch (Exception e) {
			  e.printStackTrace();
		  }
		  return stocks;
	}
	
	public List<Map<String,String>> viewDataFromPSE(){
		List<Map<String,String>> stocksList = new ArrayList<Map<String,String>>();
		try {
			// live feed from PSE
			URL url = new URL("http://pse.com.ph/stockMarket/home.html?method=getSecuritiesAndIndicesForPublic&ajax=true");
			URLConnection con = url.openConnection();
			InputStream is =con.getInputStream();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line = null;
			
			int ctr = 0;
	        while ((line = br.readLine()) != null) {
	        	String[] lineAr = line.split("\\{");
	        	for(String ln : lineAr){
	        		ctr = ctr +1;
	        		if (ctr == 1) 
	        			continue;
	        		if(ln.contains("PSEi"))
	        			break;
	        		
	        		String ln1 = ln.substring(0, ln.length()-2);
	        		String[] xAr =ln1.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
	        		
	        		Map<String,String> stockdetails = new HashMap<String,String>();
	        		for (String xl : xAr){
	        			String xlvalue = xl.replace(",", "");
	        			String[] xlvalue2 = xlvalue.split(":");
	        			stockdetails.put(xlvalue2[0].substring(1, xlvalue2[0].length()-1), xlvalue2[1].substring(1, xlvalue2[1].length()-1));
	        			//if the value has time like "07/01/2016 03:20 PM", we need this few lines of codes
	        			if(ctr == 2 && xlvalue2[0].substring(1, xlvalue2[0].length()-1).equals("securityAlias")){
	        				stockdetails.put("securityAlias",xlvalue2[1].substring(1, xlvalue2[1].length()) + ":" + xlvalue2[2].substring(0, xlvalue2[2].length()-1));
	        			}
	        		}
	        		stocksList.add(stockdetails);
	        	}
	        }
		} catch (Exception e){
			System.out.println("Something went wrong: " + e);
		}
		return stocksList;
	}

}
