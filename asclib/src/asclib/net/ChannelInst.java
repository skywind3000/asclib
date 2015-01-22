package asclib.net;

public interface ChannelInst {
	// instructions channel will receive
	final int ITMT_NEW		=	0;	// 新近外部连接：(id,tag) ip/d,port/w   <hid>
	final int ITMT_LEAVE	=	1;	// 断开外部连接：(id,tag)           <hid>
	final int ITMT_DATA		=	2;	// 外部数据到达：(id,tag) data...    <hid>
	final int ITMT_CHANNEL	=	3;	// 频道通信：(channel,tag)    <>
	final int ITMT_CHNEW	=	4;	// 频道开启：(channel,id)
	final int ITMT_CHSTOP	=	5;	// 频道断开：(channel,tag)
	final int ITMT_SYSCD	=	6;	// 系统信息：(subtype, v) data...
	final int ITMT_TIMER	=	7;	// 系统时钟：(timesec,timeusec)
	final int ITMT_UNRDAT	=	10;	// 不可靠数据包：(id,tag)
	final int ITMT_NOOP		=	80;	// 空指令：(wparam, lparam)
	//final int ITMT_BLOCK	=	99;	// 没有指令
	
	// instructions from channel to transmod
	final int ITMC_DATA		=	0;	// 外部数据发送：(id,*) data...
	final int ITMC_CLOSE	=	1;	// 关闭外部连接：(id,code)
	final int ITMC_TAG		=	2;	// 设置TAG：(id,tag)
	final int ITMC_CHANNEL	=	3;	// 组间通信：(channel,*) data...
	final int ITMC_MOVEC	=	4;	// 移动外部连接：(channel,id) data...
	final int ITMC_SYSCD	=	5;	// 系统控制消息：(subtype, v) data...
	final int ITMC_BROADCAST	=	6;	// 广播
	final int ITMC_UNRDAT	=	10;	// 不可靠数据包：(id,tag)
	final int ITMC_IOCTL	=	11;	// 连接控制指令：(id,flag)
	//final int ITMC_SEED		=	12;	// 设置加密种子
	final int ITMC_NOOP		=	80;	// 空指令：(*,*)

	// the sub instructions for the ITMC_SYSINFO
	final int ITMS_CONNC	=	0;	// 请求连接数量(st,0) cu/d,cc/d
	final int ITMS_LOGLV	=	1;	// 设置日志级别(st,level)
	final int ITMS_LISTC	=	2;	// 返回频道信息(st,cn) d[ch,id,tag],w[t,c]
	final int ITMS_RTIME	=	3;	// 系统运行时间(st,wtime)
	final int ITMS_TMVER	=	4;	// 传输模块版本(st,tmver)
	final int ITMS_REHID	=	5;	// 返回频道的(st,ch)
	final int ITMS_QUITD	=	6;	// 请求自己退出
	//final int ITMS_NODELAY	=	7;	// 设置禁用Nagle算法
	final int ITMS_TIMER	=	8;	// 设置频道零的时钟(st,timems)
	//final int ITMS_INTERVAL	=	9;	// 设置是否为间隔模式(st,isinterval)
	final int ITMS_FASTMODE	=	10;	// 设置是否启用快速模式
	final int ITMS_CHID		=	11;	// 取得自己的channel编号(st, ch)
	final int ITMS_BOOKADD	=	12;	// 增加订阅
	final int ITMS_BOOKDEL	=	13;	// 取消订阅
	final int ITMS_BOOKRST	=	14;	// 清空订阅
	final int ITMS_STATISTIC	=	15;	// 统计信息
	final int ITMS_RC4SKEY	=	16;	// 设置发送KEY (st, hid) key
	final int ITMS_RC4RKEY	=	17;	// 设置接收KEY (st, hid) key
	final int ITMS_DISABLE	=	18;	// 禁止接收该用户消息
	final int ITMS_ENABLE	=	19;	// 允许接收该用户消息
	final int ITMS_SETDOC	=	20;	// 文档设置
	final int ITMS_GETDOC	=	21;	// 文档读取
	final int ITMS_MESSAGE	=	22;	// 外部控制事件
	
	final int ITMS_NODELAY	=	1;	// 连接控制：设置立即发送模式
	final int ITMS_NOPUSH	=	2;	// 连接控制：设置数据流塞子
	final int ITMS_PRIORITY	=	3;	// SO_PRIORITY
	final int ITMS_TOS		=	4;	// IP_TOS
	
	// for log
	final int ITML_BASE		=	0x01; // 日志代码：基本
	final int ITML_INFO		=	0x02; // 日志代码：信息
	final int ITML_ERROR	=	0x04; // 日志代码：错误
	final int ITML_WARNING	=	0x08; // 日志代码：警告
	final int ITML_DATA		=	0x10; // 日志代码：数据
	final int ITML_CHANNEL	=	0x20; // 日志代码：频道
	final int ITML_EVENT	=	0x40; // 日志代码：事件
	final int ITML_LOST		=	0x80; // 日志代码：丢包记录			
}
