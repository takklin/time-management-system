import mysql.connector
import os

# 连接数据库
try:
    conn = mysql.connector.connect(
        host='localhost',
        user='root',
        password='wjx5201314',
        database='time_management'
    )
    
    cursor = conn.cursor(dictionary=True)
    
    # 查询用户和头像信息
    cursor.execute("SELECT id, username, email, avatar FROM `user` LIMIT 20")
    users = cursor.fetchall()
    
    print("=== 数据库中的用户和头像信息 ===\n")
    for user in users:
        print(f"ID: {user['id']}")
        print(f"用户名: {user['username']}")
        print(f"邮箱: {user['email']}")
        print(f"头像: {user['avatar']}")
        print("-" * 60)
    
    # 检查 uploads 目录中的文件
    print("\n=== uploads 目录中的文件 ===\n")
    if os.path.exists("uploads"):
        files = os.listdir("uploads")
        for f in files:
            print(f"  {f}")
    else:
        print("  uploads 目录不存在")
    
    # 比较数据库中的头像文件是否存在
    print("\n=== 头像文件存在性检查 ===\n")
    for user in users:
        if user['avatar']:
            # 提取文件名
            filename = user['avatar'].split('/')[-1]
            filepath = f"uploads/{filename}"
            exists = os.path.exists(filepath)
            print(f"用户 {user['username']}: {filename}")
            print(f"  路径: {filepath}")
            print(f"  存在: {'✓' if exists else '✗ 不存在'}")
            print()
    
    cursor.close()
    conn.close()
    
except Exception as e:
    print(f"错误: {e}")
