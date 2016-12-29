# -*- coding:utf-8 -*-
import os
print 'testing...'
f=open('output.txt','rb')
train_info = []
#提取出车次数、车厢数、座位数和经停站数信息
for i in range(0,4):
	train_info.append(f.readline().split())
ROUTE_NUM = int(train_info[0][0])
COACH_NUM = int(train_info[1][0])
SEAT_NUM = int(train_info[2][0])
STATION_NUM = int(train_info[3][0]) 
###############################################
buyTicket = [] # 记录各购票记录
refundTicket = [] # 记录各退票记录
remainTicket = [] # 保存各线程执行所有操作后各车次各区段总的余票
remainTicket_record = [] #通过统计除去退票后的总的买票数来确定总的余票数
Invalid_refund_Ticket = 0
Valid_refund_Ticket_fail = 0

tolerance = 20 #查票误差容忍度tolerance

for i in range(0,ROUTE_NUM+1):
	remainTicket_record.append([COACH_NUM*SEAT_NUM]*(STATION_NUM+1))
#trace信息统计
for line in f:
	if line[0:12] == 'RemainTicket':
		continue
	elif line[0:12] == 'TicketBought':#对应一条购票记录
		buyTicket.append(line[13:].split())
	elif line[0:12] == 'TicketRefund':#对应一条退票记录
		refundTicket.append(line[13:].split())
	elif line[0:17] == 'remainTicketFinal':#各区段最终余票
		remainTicket.append(line[18:].split())
	elif line[0:7] == 'Invalid':#对应一条非法退票信息
		Invalid_refund_Ticket = Invalid_refund_Ticket+1
	elif line[0:5] == 'Valid':#对应一条非法退票信息
		Valid_refund_Ticket_fail = Valid_refund_Ticket_fail+1
	else:
		continue
f.close()
#按tid大小进行排序，方便tid检查
buyTicket.sort()
refundTicket.sort()

#检查重复tid，因为tid已按大小排序，因此做前后比较就可以
tid_flag = False
check_tid = buyTicket[0][0]
for i in range(1,len(buyTicket)):
	if check_tid == buyTicket[i][0]:
		print "check_tid: ",check_tid[0] 
		print "duplicated_tid: ",buyTicket[i][0]  
		tid_flag = True
	check_tid = buyTicket[i][0]
if tid_flag != True:
	print 'No Duplicated Tid'
#删除有退票记录的票
buyTicket_check = []
for item in buyTicket:
	if item in refundTicket:
		continue
	buyTicket_check.append(item)
	for i in range(int(item[3]),int(item[4])):
		remainTicket_record[int(item[2])][i] = remainTicket_record[int(item[2])][i]-1;
		
#检查重票情况
count = 0;
time_check_i = ''
time_check_j = ''


#重票检查 直接删除有退票记录的票，再进行比对
for i in range(0,len(buyTicket_check)):
	for j in range(i+1,len(buyTicket_check)):
		if i==j:
			continue
		if  buyTicket_check[i][2] == buyTicket_check[j][2] and buyTicket_check[i][5] == buyTicket_check[j][5] and buyTicket_check[i][6] == buyTicket_check[j][6]:
			if not(buyTicket_check[i][3] >= buyTicket_check[j][4] or buyTicket_check[j][3] >= buyTicket_check[i][4]):
				count = count + 1
				print buyTicket_check[i]
				print buyTicket_check[j]
print "duplicated ticket: ",count
#检查是否有无效票退票成功				
if Invalid_refund_Ticket>0:
	print 'Invalid_refund_Ticket succeed: ',Invalid_refund_Ticket
else:
	print 'No Invalid Ticket Refund Succeed'
#检查是否有有效票退票不成功	
if Valid_refund_Ticket_fail>0:
	print 'Valid_refund_Ticket Failed: ',Valid_refund_Ticket_fail
else:
	print 'No Valid Ticket Refund Failed'
#检查查票方法的准确度
remain_flag = False
min_record = []			
for k in range(1,ROUTE_NUM+1):
	for i in range(1,STATION_NUM):
		for j in range(i+1,STATION_NUM+1):
			min_record.append(min(remainTicket_record[k][i:j]))

for k in range(0,len(remainTicket)):
	if(abs(min_record[k] - int(remainTicket[k][0]))>tolerance):#查票误差容忍度为tolerance = 20(默认允许有20张票的误差)
		print 'untolerance precision',k
		remain_flag = True
	min_record[k] = min_record[k]-int(remainTicket[k][0])

if not(remain_flag == True):
	print 'Inquiry Remain Ticket is accurate'
	
	

	


'''
#我的测试代码 备选方案 重票 + 时间戳
for i in range(0,len(buyTicket)):
	for j in range(i+1,len(buyTicket)):
		if i==j:
			continue
		
		if  buyTicket[i][2] == buyTicket[j][2] and buyTicket[i][5] == buyTicket[j][5] and buyTicket[i][6] == buyTicket[j][6]:
			if not(buyTicket[i][3] >= buyTicket[j][4] or buyTicket[j][3] >= buyTicket[i][4]):#有重票情况
				for item in refundTicket:
					if buyTicket[i][0:7]==item[0:7]:
						time_check_i = item[7]
						#item_record_i = item
					elif buyTicket[j][0:7]==item[0:7]:
						time_check_j = item[7]
						#item_record_j = item
				if time_check_i == '' and time_check_j == '':#两张票都无退票记录，确定为重票
					count = count + 1
					print buyTicket[i]
					print buyTicket[j]
					continue
				else:#有退票记录
					if (not time_check_i == '' and time_check_i < buyTicket[j][7]) or (not time_check_j == '' and time_check_j < buyTicket[i][7]):#退票记录合理
						time_check_i = ''
						time_check_j = ''
						continue
					else:#退票记录不合理
						count = count + 1
						print buyTicket[i]
						print buyTicket[j]
						time_check_i = ''
						time_check_j = ''
						continue
			else:
				continue
		else:#
			continue
'''