package com.datasmaker.datasmaker;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 *
 * @author ZjzMisaka
 *
 */

public class DatasMaker {
	//上轮添加数据是否完成
	boolean hasDoneLastInvoke = false;
	// 上轮添加数据是否成功
	boolean hasSucceedLastInvoke = true;
	// 目前可以选择的数据库
	public enum DBType{
		MySQL, Oracle
	}

	private JSch jsch;
	private Session session;
	private int assinged_port;

	private String jdbcDriver;
	private String dbUrl;

	private String dbUserName;
	private String dbPassword;

	private String tableName;

	private final String dbTypeMySQL = "com.mysql.jdbc.Driver";
	private final String dbTypeOracle = "oracle.jdbc.driver.OracleDriver";

	// 获取当前表的名字
	public String getTableName() {
		return tableName;
	}

	// 设置表名, 可以在调用结束后改变表名继续添加数据
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public DatasMaker(){
	}

	//直接连接数据库
	// 参数依次为: 数据库类型, 数据库地址, 数据库接口, 数据库名, 数据库账号, 数据库密码
	public DatasMaker(DBType dbType, String ip, int port, String dataBaseName, String dbUserName, String dbPassword){
		if (dbType == DBType.MySQL){
			this.jdbcDriver =  dbTypeMySQL;
			this.dbUrl =  "jdbc:mysql://" + ip + ":" + port + "/" + dataBaseName;
		} else if(dbType == DBType.Oracle) {
			this.jdbcDriver =  dbTypeOracle;
			this.dbUrl =  "jdbc:oracle:thin:@//" + ip + ":" + port + "/" + dataBaseName;
		}
		this.dbUrl =  "jdbc:mysql://" + ip + ":" + port + "/" + dataBaseName;
		this.dbUserName = dbUserName;
		this.dbPassword = dbPassword;
	}

	//直接连接数据库
	// 参数依次为: 数据库类型, 数据库地址, 数据库接口, 数据库名, 数据库账号, 数据库密码, 数据库表名
	public DatasMaker(DBType dbType, String ip, int port, String dataBaseName, String dbUserName, String dbPassword, String tableName){
		if (dbType == DBType.MySQL){
			this.jdbcDriver =  dbTypeMySQL;
			this.dbUrl =  "jdbc:mysql://" + ip + ":" + port + "/" + dataBaseName;
		} else if(dbType == DBType.Oracle) {
			this.jdbcDriver =  dbTypeOracle;
			this.dbUrl =  "jdbc:oracle:thin:@//" + ip + ":" + port + "/" + dataBaseName;
		}
		this.dbUserName = dbUserName;
		this.dbPassword = dbPassword;
		this.setTableName(tableName);
	}

	// 通过ssh连接数据库
	// 参数依次为: 数据库类型, 地址, ssh端口, 本地端口, 数据库端口, ssh用户名, ssh密码, 数据库用户名, 数据库密码 
	public DatasMaker(DBType dbType, String ip, int sshPort, int localPort, int dbPort, String sshUserName, String sshPassword, String dataBaseName, String dbUserName, String dbPassword){
		if (dbType == DBType.MySQL){
			this.jdbcDriver =  dbTypeMySQL;
			this.dbUrl =  "jdbc:mysql://localhost:" + localPort + "/" + dataBaseName;
		} else if(dbType == DBType.Oracle) {
			this.jdbcDriver =  dbTypeOracle;
			this.dbUrl =  "jdbc:oracle:thin:@//localhost:" + localPort + "/" + dataBaseName;
		}
		this.dbUserName = dbUserName;
		this.dbPassword = dbPassword;

		jsch = new JSch();
		try {
			session = jsch.getSession(sshUserName, ip, sshPort);
			session.setPassword(sshPassword);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();
			//这里打印SSH服务器版本信息
			System.out.println(session.getServerVersion());
			// 设置SSH本地端口转发,本地转发到远程
			assinged_port = session.setPortForwardingL(localPort, ip, dbPort);
			System.out.println("localhost:" + assinged_port);
		} catch (JSchException e) {
			e.printStackTrace();
		}
	}

	// 通过ssh连接数据库
	// 参数依次为: 数据库类型, 地址, ssh端口, 本地端口, 数据库端口, ssh用户名, ssh密码, 数据库用户名, 数据库密码, 表名 
	public DatasMaker(DBType dbType, String ip, int sshPort, int localPort, int dbPort, String sshUserName, String sshPassword, String dataBaseName, String dbUserName, String dbPassword, String tableName){
		if (dbType == DBType.MySQL){
			this.jdbcDriver =  dbTypeMySQL;
			this.dbUrl =  "jdbc:mysql://localhost:" + localPort + "/" + dataBaseName;
		} else if(dbType == DBType.Oracle) {
			this.jdbcDriver =  dbTypeOracle;
			this.dbUrl =  "jdbc:oracle:thin:@//localhost:" + localPort + "/" + dataBaseName;
		}
		this.dbUserName = dbUserName;
		this.dbPassword = dbPassword;
		this.setTableName(tableName);

		jsch = new JSch();
		try {
			session = jsch.getSession(sshUserName, ip, sshPort);
			session.setPassword(sshPassword);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();
			//这里打印SSH服务器版本信息
			System.out.println(session.getServerVersion());
			// 设置SSH本地端口转发,本地转发到远程
			assinged_port = session.setPortForwardingL(localPort, ip, dbPort);
			System.out.println("localhost:" + assinged_port);
		} catch (JSchException e) {
			e.printStackTrace();
		}
	}

	// methodName为此方法需要调用的制造数据的方法的名字, 如"makeData", callerClassName为制造数据的方法所属的类的名字, 如"com.test.DataMakerTest".
	// 制造数据方法和它的所属类的访问修饰符必须为public.
	public void makeDatas(int allDataTotalCount, int oneTurnDataTotalCount, String fields, String callerClassName, String methodName){
		Class<?> callerCalss;
		Object classObj = null;
		Method method = null;
		try {
			callerCalss = Class.forName(callerClassName);
			// 获取类
			classObj = callerCalss.newInstance();
			// 获取方法
			method = classObj.getClass().getDeclaredMethod(methodName, boolean.class, boolean.class);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException e2) {
			e2.printStackTrace();
		}

		StringBuffer sqlDatas;
		int dataCountNow = 1;

		Connection conn = null;
		Statement stmt = null;

		int dataCountThisTurnNow = 1;

		try{
			// 加载驱动
			Class.forName(jdbcDriver);
			DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
		} catch (SQLException | ClassNotFoundException e1) {
			e1.printStackTrace();
		}

		// 添加数据条数少于需要的条数, 开始新的一轮添加
		while (dataCountNow <= allDataTotalCount){
			//当下批次上传数据后数据量会超过需要的条数, 将这批次的数量改为剩下的条数.
			if(allDataTotalCount - dataCountNow < oneTurnDataTotalCount) {
				oneTurnDataTotalCount = allDataTotalCount - dataCountNow + 1;
			}

			dataCountThisTurnNow = 1;
			sqlDatas = new StringBuffer();
			try{
				conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
				stmt = conn.createStatement();
			} catch (SQLException e1) {
				e1.printStackTrace();
				continue;
			}

			while (dataCountThisTurnNow <= oneTurnDataTotalCount){
				String result = null;
				try {
					// 调用数据获取方法
					result = (String)method.invoke(classObj, hasDoneLastInvoke, hasSucceedLastInvoke);
					hasDoneLastInvoke = false;
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
					System.out.println("遇到未知错误, 退出程序");
					return;
				}

				hasSucceedLastInvoke = true;

				// 拼接获取到的数据
				sqlDatas.append("(" + result + "),");
				System.out.println(dataCountNow + "/" + allDataTotalCount + "\t\t\t" + String.format("%.6f", dataCountNow / (allDataTotalCount * 1.0)) + "\t\t\t" + dataCountThisTurnNow + "/" + oneTurnDataTotalCount + "\t\t\t" + String.format("%.6f", dataCountThisTurnNow / (oneTurnDataTotalCount * 1.0)));
				++dataCountThisTurnNow;
				++dataCountNow;
			}
			// 去除拼接完毕的数据字符串最后多余的逗号
			sqlDatas.deleteCharAt(sqlDatas.length() - 1);
			try{
				// 执行添加数据的sql语句, 一轮添加完成.
				stmt.executeUpdate("INSERT INTO `" + tableName +"` (" + fields + ") VALUES " + sqlDatas.toString());
			} catch (SQLException e) {
				// 当某条数据不合法, 这次添加不作数, 重新获取数据.
				e.printStackTrace();
				System.out.println("遇到错误, 重新获取数据");
				dataCountNow -= oneTurnDataTotalCount;
				// 添加数据不成功, hasSucceedLastInvoke置为false, 在下次invoke方法调用时传递给制造数据的方法
				hasDoneLastInvoke = true;
				hasSucceedLastInvoke = false;
			}

			try {
				stmt.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
	}

	public void makeDatas(int allDataTotalCount, String fields, String callerClassName, String methodName){
		int maxAllowedPacket = 0;

		Class<?> callerCalss;
		Object classObj = null;
		Method method = null;
		try {
			callerCalss = Class.forName(callerClassName);
			// 获取类
			classObj = callerCalss.newInstance();
			// 获取方法
			method = classObj.getClass().getDeclaredMethod(methodName, boolean.class, boolean.class, int.class);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException e2) {
			e2.printStackTrace();
		}

		StringBuffer sqlDatas;
		int dataCountNow = 0;

		Connection conn = null;
		Statement stmt = null;

		int dataCountThisTurnNow = 0;
		int dataCountThisTurnNowopy = 0;

		try{
			// 加载驱动
			Class.forName(jdbcDriver);
			DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
		} catch (SQLException | ClassNotFoundException e1) {
			e1.printStackTrace();
		}


		//获取数据库允许的最大数据包大小
		try {
			conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
			stmt = conn.createStatement();
			ResultSet resultMaxAllowedPacket = stmt.executeQuery("show VARIABLES like '%max_allowed_packet%';");
			resultMaxAllowedPacket.next();
			maxAllowedPacket = resultMaxAllowedPacket.getInt("Value");
			stmt.close();
			conn.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		// 添加数据总数少于需要的条数, 开始新的一轮添加
		while (dataCountNow < allDataTotalCount){
			dataCountThisTurnNowopy = dataCountThisTurnNow;
			dataCountThisTurnNow = 0;

			sqlDatas = new StringBuffer();
			try{
				conn = DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
				stmt = conn.createStatement();
			} catch (SQLException e1) {
				e1.printStackTrace();
				continue;
			}

			while (dataCountNow < allDataTotalCount){
				String result = null;
				try {
					// 调用数据获取方法
					result = (String)method.invoke(classObj, hasDoneLastInvoke, hasSucceedLastInvoke, dataCountThisTurnNowopy);
					dataCountThisTurnNowopy = 0;
					hasDoneLastInvoke = false;
					hasSucceedLastInvoke = true;
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
					System.out.println("遇到未知错误, 退出程序");
					return;
				}

				// 如果拼接后的数据长度不大于允许的最大数据包大小, 拼接获取到的数据
				if(sqlDatas.length() + 27 + tableName.length() + fields.length() + result.length() <= maxAllowedPacket){
					sqlDatas.append("(" + result + "),");
					++dataCountThisTurnNow;
					++dataCountNow;
					System.out.println(dataCountNow + "/" + allDataTotalCount + "\t\t\t" + String.format("%.6f", dataCountNow / (allDataTotalCount * 1.0)));
				} else {
					break;
				}
			}
			// 去除拼接完毕的数据字符串最后多余的逗号
			sqlDatas.deleteCharAt(sqlDatas.length() - 1);
			try{
				// 执行添加数据的sql语句, 一轮添加完成.
				stmt.executeUpdate("INSERT INTO `" + tableName +"` (" + fields + ") VALUES " + sqlDatas.toString());
			} catch (SQLException e) {
				// 当某条数据不合法, 这次添加不作数, 重新获取数据.
				e.printStackTrace();
				System.out.println("遇到错误, 重新获取数据");
				dataCountNow -= dataCountThisTurnNow;
				// 添加数据不成功, hasSucceedLastInvoke置为false, 在下次invoke方法调用时传递给制造数据的方法
				hasSucceedLastInvoke = false;
			}

			try {
				stmt.close();
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}

			hasDoneLastInvoke = true;
		}
	}
}