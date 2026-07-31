"""生成假数据到 PostgreSQL"""
import psycopg2
import random
import string

random.seed(42)

conn = psycopg2.connect(
    host="localhost", port=5432, dbname="scau_archive",
    user="postgres", password="123456"
)
cur = conn.cursor()

# 清空
cur.execute("TRUNCATE TABLE admission_fact, graduation_fact, student_fact RESTART IDENTITY CASCADE;")

# 读取维度数据
cur.execute("SELECT province_id FROM province_dim;")
province_ids = [r[0] for r in cur.fetchall()]
cur.execute("SELECT major_id FROM major_dim;")
major_ids = [r[0] for r in cur.fetchall()]
cur.execute("SELECT degree_id FROM degree_dim;")
degree_ids = [r[0] for r in cur.fetchall()]
cur.execute("SELECT dest_id FROM destination_dim;")
dest_ids = [r[0] for r in cur.fetchall()]

# 姓名素材
surnames = "陈张李王刘杨黄赵周吴徐孙马朱胡林郭何高罗郑梁谢宋唐韩曹许邓冯"
given_names = "伟芳娜敏静丽强磊军洋勇艳杰娟涛明超霞平刚文华飞斌玲国强"

def random_name():
    return random.choice(surnames) + ''.join(random.choices(given_names, k=random.randint(1,2)))

years = [2020, 2021, 2022, 2023, 2024, 2025]
base_stu = 0

for y in years:
    # 每年人数：先升后降
    if y == 2020: n = 40
    elif y == 2021: n = 45
    elif y == 2022: n = 50
    elif y == 2023: n = 55
    elif y == 2024: n = 48
    else: n = 42

    for i in range(n):
        base_stu += 1
        student_no = f"{y}{base_stu:03d}"
        exam_no = f"{y}{base_stu:05d}"
        name = random_name()
        gender = random.choice(['男', '女'])
        id_card = f"{200000+base_stu:017d}{random.choice('0123456789X')}"
        prov_id = 19 if random.random() < 0.35 else random.choice(province_ids)
        maj_id = random.choice(major_ids)

        # 录取分数（按专业）
        score_map = {1:580, 2:575, 3:565, 4:555, 5:560, 6:570, 7:550, 8:545}
        score = score_map.get(maj_id, 560) + random.randint(0, 29)

        # 培养层次：本科为主，少量硕士/博士（确定性分配，不消耗 random 序列）
        level_id = 1 if base_stu % 10 < 8 else (2 if base_stu % 10 == 8 else 3)

        # 录取
        cur.execute(
            "INSERT INTO admission_fact (student_no, exam_no, name, id_card, gender, province_id, major_id, admission_score, admission_date, degree_id) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)",
            (student_no, exam_no, name, id_card, gender, prov_id, maj_id, score, f"{y}-09-01", level_id)
        )

        # 学籍
        cur.execute(
            "INSERT INTO student_fact (student_no, name, id_card, gender, major_id, province_id, admission_date, degree_id) VALUES (%s,%s,%s,%s,%s,%s,%s,%s)",
            (student_no, name, id_card, gender, maj_id, prov_id, f"{y}-09-01", level_id)
        )

        # 毕业（2020-2023届）
        if y <= 2023:
            grad_date = f"{y+4}-06-30"
            # 学位按专业分类
            deg_map = {1:20, 2:20, 3:20, 4:20, 5:22, 6:22, 7:23, 8:25}
            deg_id = deg_map.get(maj_id, random.choice(degree_ids))
            # 去向：50%就业 30%升学 20%其他
            r = random.random()
            if r < 0.5: dest_id = 1
            elif r < 0.8: dest_id = 2
            else: dest_id = random.choice([d for d in dest_ids if d not in (4,5)])

            cur.execute(
                "INSERT INTO graduation_fact (student_no, name, id_card, gender, degree_id, dest_id, graduation_date) VALUES (%s,%s,%s,%s,%s,%s,%s)",
                (student_no, name, id_card, gender, deg_id, dest_id, grad_date)
            )
            cur.execute("UPDATE student_fact SET graduated = true WHERE student_no = %s", (student_no,))

conn.commit()

# 验证
cur.execute("SELECT 'admission_fact', count(*) FROM admission_fact UNION ALL SELECT 'student_fact', count(*) FROM student_fact UNION ALL SELECT 'graduation_fact', count(*) FROM graduation_fact;")
for row in cur.fetchall():
    print(f"{row[0]}: {row[1]} rows")

cur.close()
conn.close()
