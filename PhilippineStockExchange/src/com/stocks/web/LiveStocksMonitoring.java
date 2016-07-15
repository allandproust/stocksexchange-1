package com.stocks.web;


import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.stocks.scheduled.jobs.BuySellTask;
import com.stocks.scheduled.jobs.MyStockTask;
import java.util.Date;
import java.util.Timer;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LiveStocksMonitoring extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws IOException, ServletException {
		try{
			DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			DateFormat dateFormatter2 = new SimpleDateFormat("yyyy-MM-dd");
			
			String tradingDate = dateFormatter2.format(new Date());
			
			String startTime = tradingDate + " 09:40:00";//this is original. put this back after testing.
			//String startTime = tradingDate + " 10:50:00"; //testing
			Date date = dateFormatter.parse(startTime);

			Timer timer = new Timer();
			
			System.out.println("1% Trading Date: " + tradingDate);
			System.out.println("1% Start Time: " + startTime);
			//timer.schedule(new MyStockTask(), date, 10000);
			// 1 second = 1,000 milli seconds
			// 1 minute = 60 seconds = 60,000 milli seconds
			// 10 minutes = 600 seconds = 600,000
			timer.scheduleAtFixedRate(new MyStockTask(), date, 1800000);
			System.out.println("1% Live Stocks monitoring has started!");

			Timer timer2 = new Timer();
			timer2.scheduleAtFixedRate(new BuySellTask(), date, 120000);
			System.out.println("1% Buy and Sell Stocks monitoring has started!");
			
		} catch (Exception ex){
			System.out.println("1% Exception: " + ex.getMessage());
		}
		RequestDispatcher view = request.getRequestDispatcher("LiveStocksMonitoring.jsp");
		view.forward(request,response);
	}
}