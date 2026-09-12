"""论文图源修订：从原始mxGraph单元继承样式与标识，输出独立副本。
仅处理paper材料，不导入应用、不连接数据库。PNG/SVG/XML共用同一节点和线路定义。
"""
from pathlib import Path
from copy import deepcopy
from zipfile import ZipFile, ZIP_DEFLATED
import xml.etree.ElementTree as ET
import hashlib, html, json, math, re
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
PAPER = HERE.parent
OUT = HERE / 'figures'
OUT.mkdir(parents=True, exist_ok=True)
BLUE, GREEN, ORANGE, PURPLE = '#1B4F72', '#14816B', '#B95E00', '#6C3483'
INK = '#17354A'
FONT = 'C:/Windows/Fonts/msyh.ttc'
BOLD = 'C:/Windows/Fonts/msyhbd.ttc'
REPORT = []

def sha(b): return hashlib.sha256(b).hexdigest()
def styles(s): return dict(x.split('=',1) if '=' in x else (x,'1') for x in s.split(';') if x)
def st(s): return ';'.join(f'{k}={v}' for k,v in s.items())+';'

class Figure:
    def __init__(self, number, name, h):
        self.number,self.name,self.w,self.h=number,name,1100,h
        self.source=PAPER/f'图{number}.xml'
        self.base=ET.parse(self.source)
        self.original={c.get('id'):c for c in self.base.findall('.//mxCell')}
        self.background=[];self.nodes=[];self.edges=[];self.audit=[]
    def node(self, ident, template, box, text='', size=22, bold=False, fill=None, stroke=None, color=None, bg=False, shape=None, align='center', dash=False):
        c=deepcopy(self.original[template])
        c.attrib={'id':ident,'value':html.escape(text).replace('\n','<br>'), 'vertex':'1','parent':'1','style':c.get('style','')}
        s=styles(c.get('style',''))
        for k in ['rotation','spacingLeft','spacingTop','spacingBottom','spacingRight','aspect']:s.pop(k,None)
        s.update(html='1',whiteSpace='wrap',fontSize=str(size),fontFamily='Microsoft YaHei',fontStyle='1' if bold else '0',align=align,verticalAlign='middle',spacing='8')
        if fill is not None:s['fillColor']=fill
        if stroke is not None:s['strokeColor']=stroke
        if color is not None:s['fontColor']=color
        s.setdefault('fontColor',INK)
        if shape:
            for k in ['shape','ellipse','text','cylinder','cylinder3']:s.pop(k,None)
            if shape=='text':s.update(text='1',strokeColor='none',fillColor='none')
            elif shape=='cylinder':s.update(shape='cylinder',size='14')
            elif shape=='rect':s['rounded']='0'
            else:s.update(rounded='1',arcSize='12')
        s['dashed']='1' if dash else '0'
        if s.get('strokeColor')!='none':s['strokeWidth']='2'
        c.set('style',st(s))
        for child in list(c):c.remove(child)
        ET.SubElement(c,'mxGeometry',attrib={**dict(zip(('x','y','width','height'),map(str,box))),'as':'geometry'})
        (self.background if bg else self.nodes).append(c)
        self.audit.append({'id':ident,'source_cell':template,'value':text,'box':box})
        return ident
    def edge(self, ident, source, target, points, color=BLUE, dash=False, arrow=True):
        # Explicit port coordinates and waypoints keep the editable diagram and exports identical.
        allnodes={c.get('id'):c for c in self.background+self.nodes}
        s=dict(edgeStyle='none',rounded='0',html='1',strokeColor=color,strokeWidth='2',endArrow='block' if arrow else 'none',endFill='1',dashed='1' if dash else '0')
        for name,prefix,pt in [(source,'exit',points[0]),(target,'entry',points[-1])]:
            if name:
                g=allnodes[name].find('mxGeometry');x,y,w,h=[float(g.get(a)) for a in ('x','y','width','height')]
                s.update({prefix+'X':str((pt[0]-x)/w),prefix+'Y':str((pt[1]-y)/h),prefix+'Dx':'0',prefix+'Dy':'0',prefix+'Perimeter':'0'})
        c=ET.Element('mxCell',id=ident,style=st(s),edge='1',parent='1')
        if source:c.set('source',source)
        if target:c.set('target',target)
        g=ET.SubElement(c,'mxGeometry',relative='1',attrib={'as':'geometry'})
        ET.SubElement(g,'mxPoint',x=str(points[0][0]),y=str(points[0][1]),attrib={'as':'sourcePoint'})
        ET.SubElement(g,'mxPoint',x=str(points[-1][0]),y=str(points[-1][1]),attrib={'as':'targetPoint'})
        if len(points)>2:
            ar=ET.SubElement(g,'Array',attrib={'as':'points'})
            for x,y in points[1:-1]:ET.SubElement(ar,'mxPoint',x=str(x),y=str(y))
        self.edges.append((c,points,color,dash,arrow))
    def label(self, ident, text, x,y,w,color=BLUE,size=20):
        return self.node(ident,next(iter(k for k,c in self.original.items() if c.get('vertex'))),(x,y,w,28),text,size,color=color,shape='text')
    def save(self):
        tree=deepcopy(self.base);dg=tree.find('.//diagram');dg.set('name',self.name);dg.set('id',self.name)
        model=tree.find('.//mxGraphModel');model.set('pageWidth',str(self.w));model.set('pageHeight',str(self.h));model.set('background','#FFFFFF')
        root=model.find('root');root.clear();ET.SubElement(root,'mxCell',id='0');ET.SubElement(root,'mxCell',id='1',parent='0')
        for c in self.background+[e[0] for e in self.edges]+self.nodes:root.append(c)
        ET.indent(tree,space='  ')
        data=ET.tostring(tree.getroot(),encoding='utf-8',xml_declaration=True)
        (OUT/(self.name+'.drawio')).write_bytes(data);(OUT/(self.name+'.xml')).write_bytes(data)
        self.render()
        REPORT.append({'figure':self.name,'source':self.source.name,'source_sha256':sha(self.source.read_bytes()),'nodes':self.audit,'edges':[e[0].get('id') for e in self.edges]})
    def render(self):
        scale=3
        im=Image.new('RGB',(self.w*scale,self.h*scale),'white');draw=ImageDraw.Draw(im)
        svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.w}" height="{self.h}" viewBox="0 0 {self.w} {self.h}"><rect width="100%" height="100%" fill="white"/>']
        def line(points,color,width=2,dash=False):
            for a,b in zip(points,points[1:]):
                if dash:
                    length=math.dist(a,b)
                    for d in range(0,int(length),12):
                        e=min(d+7,length)
                        if length:draw.line([(scale*(a[0]+(b[0]-a[0])*q/length),scale*(a[1]+(b[1]-a[1])*q/length)) for q in (d,e)],fill=color,width=max(1,int(width*scale)))
                else:draw.line([(x*scale,y*scale) for x,y in (a,b)],fill=color,width=max(1,int(width*scale)))
            svg.append(f'<polyline points="'+ ' '.join(f'{x},{y}' for x,y in points)+f'" fill="none" stroke="{color}" stroke-width="{width}"'+(' stroke-dasharray="7 5"' if dash else '')+'/>')
        def node(c):
            s=styles(c.get('style'));g=c.find('mxGeometry');x,y,w,h=[float(g.get(k)) for k in ('x','y','width','height')]
            fill=s.get('fillColor','#FFFFFF');stroke=s.get('strokeColor',BLUE);textonly='text' in s or (fill=='none' and stroke=='none')
            if not textonly:
                box=(x*scale,y*scale,(x+w)*scale,(y+h)*scale);fc=None if fill=='none' else fill;sc=None if stroke=='none' else stroke
                cylinder=s.get('shape')=='cylinder';radius=12 if s.get('rounded')=='1' else 0
                if cylinder:
                    ry=12
                    draw.rectangle((x*scale,(y+ry)*scale,(x+w)*scale,(y+h-ry)*scale),fill=fc)
                    draw.ellipse((x*scale,(y+h-2*ry)*scale,(x+w)*scale,(y+h)*scale),fill=fc,outline=sc,width=2*scale)
                    draw.rectangle((x*scale,(y+ry)*scale,(x+w)*scale,(y+h-ry)*scale),fill=fc)
                    draw.line([(x*scale,(y+ry)*scale),(x*scale,(y+h-ry)*scale)],fill=sc,width=2*scale);draw.line([((x+w)*scale,(y+ry)*scale),((x+w)*scale,(y+h-ry)*scale)],fill=sc,width=2*scale)
                    draw.ellipse((x*scale,y*scale,(x+w)*scale,(y+2*ry)*scale),fill=fc,outline=sc,width=2*scale)
                    svg.append(f'<path d="M{x},{y+ry} A{w/2},{ry} 0 0 1 {x+w},{y+ry} L{x+w},{y+h-ry} A{w/2},{ry} 0 0 1 {x},{y+h-ry} Z" fill="{fill}" stroke="{stroke}" stroke-width="2"/><ellipse cx="{x+w/2}" cy="{y+ry}" rx="{w/2}" ry="{ry}" fill="{fill}" stroke="{stroke}" stroke-width="2"/>')
                else:
                    draw.rounded_rectangle(box,radius=radius*scale,fill=fc,outline=None if s.get('dashed')=='1' else sc,width=2*scale)
                    if s.get('dashed')=='1':line([(x,y),(x+w,y),(x+w,y+h),(x,y+h),(x,y)],stroke,dash=True)
                    svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{radius}" fill="{fill}" stroke="{stroke}" stroke-width="2"'+(' stroke-dasharray="7 5"' if s.get('dashed')=='1' else '')+'/>')
            text=html.unescape(c.get('value','').replace('<br>','\n'))
            if not text:return
            fs=float(s['fontSize']);font=ImageFont.truetype(BOLD if s.get('fontStyle')=='1' else FONT,round(fs*scale));col=s.get('fontColor',INK)
            lines=text.split('\n');step=fs*1.35
            # Fail before delivery instead of quietly shrinking text.
            assert max(draw.textlength(t,font=font)/scale for t in lines) <= w-8,(self.name,c.get('id'),'text too wide',text)
            assert len(lines)*step <= h+8,(self.name,c.get('id'),'text too high',text)
            for i,t in enumerate(lines):
                yy=y+h/2+(i-(len(lines)-1)/2)*step
                xx=x+12 if s.get('align')=='left' else x+w/2
                anchor='lm' if s.get('align')=='left' else 'mm'
                draw.text((xx*scale,yy*scale),t,font=font,fill=col,anchor=anchor)
                svg.append(f'<text x="{xx}" y="{yy}" dominant-baseline="central" text-anchor="'+('start' if anchor=='lm' else 'middle')+f'" font-family="Microsoft YaHei, sans-serif" font-size="{fs}" font-weight="'+('700' if s.get('fontStyle')=='1' else '400')+f'" fill="{col}">{html.escape(t)}</text>')
        for c in self.background:node(c)
        for c,pts,col,dash,arrow in self.edges:
            line(pts,col,dash=dash)
            if arrow:
                a,b=pts[-2:];ang=math.atan2(b[1]-a[1],b[0]-a[0]);back=(b[0]-10*math.cos(ang),b[1]-10*math.sin(ang));verts=[b,(back[0]-5*math.sin(ang),back[1]+5*math.cos(ang)),(back[0]+5*math.sin(ang),back[1]-5*math.cos(ang))]
                draw.polygon([(x*scale,y*scale) for x,y in verts],fill=col);svg.append('<polygon points="'+' '.join(f'{x},{y}' for x,y in verts)+f'" fill="{col}"/>')
        for c in self.nodes:node(c)
        im.save(OUT/(self.name+'.png'));svg.append('</svg>');(OUT/(self.name+'.svg')).write_text('\n'.join(svg),encoding='utf-8')

def architecture():
    f=Figure(3,'fig3-1',670)
    f.node('L1','L1',(25,10,1050,125),bg=True)
    f.node('L1-title','L1-title',(45,17,340,30),'表现层  Vue 3 / Element Plus',23,True,color=BLUE)
    for i,(txt,x) in enumerate([('档案上传与任务状态',45),('业务数据与统计图表',392),('AI 助手与知识库',739)],1):f.node(f'm{i}',f'm{i}',(x,60,315,54),txt,22,color=BLUE)
    f.node('L2','L2',(25,177,1050,110),bg=True)
    f.node('L2-title','L2-title',(45,184,600,31),'业务层  Spring Boot / MyBatis-Plus',23,True,color=BLUE)
    f.node('p1','p1',(45,231,230,40),'任务创建与进程管理',21,color=BLUE)
    f.node('p2','p2',(297,231,230,40),'字段处理与持久化',21,color=BLUE)
    f.node('p3','p3',(549,231,230,40),'文件归档与质量参考分',20,color=BLUE)
    f.node('p4','p4',(801,231,252,40),'业务查询与报告',21,color=BLUE)
    f.node('L3','L3',(25,339,700,155),bg=True,fill='#EBF5FB',stroke=BLUE)
    f.node('L3-title','L3-title',(285,346,195,30),'Python 组件',22,True,color=BLUE)
    f.node('ai2','ai2',(45,391,305,81),'任务子进程\nOCR / 视觉提取 / 预测',22,color=GREEN)
    f.node('ai1','ai1',(395,391,310,81),'FastAPI 辅助进程\nAgent / RAG / 数据库工具',21,color=BLUE)
    f.node('DB','DB',(780,339,295,155),bg=True)
    f.node('DB-title','DB-title',(800,345,250,30),'PostgreSQL',24,True,color=ORANGE)
    f.node('db1','db1',(800,394,250,77),'业务记录与任务日志\n知识文本与向量',21,color=ORANGE,shape='round')
    f.node('redis','db2',(35,563,300,85),'Redis\n临时状态与查询缓存',22,fill='#FEF5E7',stroke=ORANGE,color=ORANGE)
    f.node('external','ai3',(450,563,575,85),'外部模型服务\n视觉提取 / 文本生成 / 向量嵌入',22,color='#922B21')
    f.edge('e12','L1','L2',[(550,135),(550,177)]);f.label('e12-text','HTTP / SSE',555,140,170,size=19)
    f.edge('e2task','L2','ai2',[(195,287),(195,391)]);f.label('e2task-text','启动 / 读取结果',202,300,260,size=19)
    f.edge('e2api','L2','ai1',[(550,287),(550,391)]);f.label('e2api-text','管理 + HTTP / SSE',558,300,303,size=19)
    f.edge('e2db','L2','DB',[(930,287),(930,339)],ORANGE);f.label('e2db-text','JDBC',935,300,110,ORANGE,19)
    f.edge('eaidb','ai1','db1',[(705,432),(800,432)],ORANGE);f.label('eaidb-text','SQL',717,399,75,ORANGE,18)
    f.edge('e2redis','L2','redis',[(25,260),(10,260),(10,605),(35,605)],ORANGE)
    f.edge('evision','ai2','external',[(200,472),(200,526),(600,526),(600,563)],'#C0392B');f.label('vision-label','视觉路径调用',237,496,240,'#922B21',19)
    f.edge('eagent','ai1','external',[(550,472),(550,507),(875,507),(875,563)],'#C0392B');f.label('agent-label','文本 / 嵌入调用',700,519,320,'#922B21',19)
    f.save()

def ingestion():
    f=Figure(1,'fig3-2',650)
    f.node('n1','n1',(285,12,530,64),'档案文件输入并创建任务\nCSV / Excel / 图像 / PDF',22,True,color=BLUE)
    f.node('n2','n2',(345,108,410,53),'按扩展名和 useLlm 选择路径',22,True,color=BLUE)
    f.node('n2-note','n2-note',(805,105,270,60),'图像 / PDF 由用户选路\n不按清晰度自动分流',19,color=BLUE)
    f.node('zone1','zone1',(25,198,1050,253),bg=True,fill='#F8FBFD',stroke='#A8C6D8')
    for ident,temp,x,title,detail in [('csv','n3-ocr',55,'CSV / Excel','结构化解析\n列名适配与基础校验'),('n3-ocr','n3-ocr',400,'OCR 路径','PPStructureV3 表格识别\n值特征校正与表头映射'),('n3-mllm','n3-mllm',745,'MLLM 路径','动态字段提示词\n视觉模型生成 JSON')]:
        col=BLUE if ident=='csv' else GREEN if ident=='n3-ocr' else ORANGE
        f.node(ident,temp,(x,223,300,64),title,24,True,color=col,stroke=col,fill='#EBF5FB' if ident=='csv' else None)
        f.node(ident+'-detail',temp,(x,320,300,95),detail,21,color=col,stroke=col,fill='#FFFFFF')
        f.edge(ident+'-next',ident,ident+'-detail',[(x+150,287),(x+150,320)],col)
        f.edge('route-'+ident,'n2',ident,[(550,161),(550,184),(x+150,184),(x+150,223)],col)
    f.node('n4','n4',(255,488,590,58),'业务持久化：新增或更新匹配记录',23,True,color=BLUE)
    f.node('n5','n5',(255,579,590,54),'记录质量参考分、文件归档与任务终态',22,color=BLUE)
    f.edge('e12','n1','n2',[(550,76),(550,108)])
    for ident,x,col in [('csv',205,BLUE),('n3-ocr',550,GREEN),('n3-mllm',895,ORANGE)]:f.edge('merge-'+ident,ident+'-detail','n4',[(x,415),(x,467),(550,467),(550,488)],col)
    f.edge('e45','n4','n5',[(550,546),(550,579)])
    f.save()

def database():
    f=Figure(6,'fig3-3',680)
    f.node('dim-zone','dim-zone',(20,10,1060,132),bg=True)
    f.node('dim-title','dim-title',(40,16,510,30),'基础维度与业务支持',24,True,color=ORANGE)
    f.node('d1','d1',(40,60,650,65),'college_dim / major_dim / class_dim\nprovince_dim / degree_dim / destination_dim',20,color=ORANGE)
    f.node('d-more','d-more',(720,60,335,65),'archive_file_dim\n来源文件',21,color=ORANGE)
    f.node('fact-zone','fact-zone',(20,185,1060,208),bg=True)
    f.node('fact-title','fact-title',(260,191,240,30),'业务事实记录',22,True,color=BLUE)
    for i,(name,fields,x,col) in enumerate([('admission_fact','PK  id\nstudent_no / exam_no\nprovince_id / major_id\ndegree_id / file_id',40,'#2E86AB'),('student_fact','PK  id\nstudent_no / id_card\nmajor_id / class_id\ngraduated',405,BLUE),('graduation_fact','PK  id\nstudent_no / degree_id\ndest_id / file_id\ngraduation_date',770,'#377DAD')],1):
        f.node(f'f{i}',f'f{i}',(x,232,290,142),bg=True,stroke=col)
        f.node(f'f{i}-h',f'f{i}-h',(x,232,290,34),name,21,True,fill=col,color='#FFFFFF')
        f.node(f'f{i}-b',f'f{i}-b',(x+8,270,274,100),fields,18,color=BLUE)
    f.edge('dimension-admission','d1','f1',[(220,125),(220,160),(185,160),(185,232)],ORANGE,True)
    f.edge('dimension-student','d1','f2',[(535,125),(535,160),(550,160),(550,232)],ORANGE,True)
    f.edge('file-graduation','d-more','f3',[(885,125),(885,160),(915,160),(915,232)],ORANGE,True)
    f.node('meta','meta',(20,425,1060,70),bg=True)
    f.node('meta-title','meta-title',(40,432,330,52),'metadata_standard\nPK  metadata_id',20,True,color=GREEN)
    f.node('meta-fields','meta-fields',(390,434,655,48),'field_code / field_name / source_field\nfield_type / is_required',20,color=GREEN)
    f.node('kb-zone','kb-zone',(20,528,1060,112),bg=True)
    f.node('kb1','kb1',(40,548,365,72),'knowledge_base\nPK  id · title · status',21,color=PURPLE)
    f.node('kb2','kb2',(660,548,395,72),'knowledge_chunks\nPK  id · FK  kb_id · vector(1024)',19,color=PURPLE)
    f.edge('ekb','kb1','kb2',[(405,585),(660,585)],PURPLE);f.label('ekb-label','1 : N  外键关联',416,548,232,PURPLE,20)
    f.label('leg','虚线：服务层维护的逻辑关联；知识库为独立入口。完整表清单见表3-1。',35,647,1030,INK,19)
    f.save()

def deployment():
    f=Figure(5,'fig3-4',530)
    f.node('docker','docker',(15,10,1070,405),bg=True)
    f.node('docker-badge','docker-badge',(35,20,235,34),'Docker Compose',23,True)
    f.node('docker-sub','docker-sub',(290,20,730,34),'四个服务：frontend · backend · db · redis',22,color=BLUE)
    f.node('c1','c1',(40,107,205,147),bg=True)
    f.node('c1-title','c1-title',(50,122,185,35),'frontend',24,True,color=GREEN)
    f.node('c1-i1','c1-i1',(53,171,179,62),'Nginx\nVue 静态资源',22,color=GREEN)
    f.node('c2','c2',(352,86,407,278),bg=True,fill='#EBF5FB')
    f.node('c2-title','c2-title',(367,96,377,37),'backend  Java + Python',23,True,color=BLUE)
    f.node('c2-i1','c2-i1',(372,148,367,56),'Spring Boot 业务服务',23,True,color=BLUE)
    f.node('task','c4-i2',(372,259,171,79),'任务子进程\nOCR / 视觉 / 预测',18,color=GREEN,stroke=GREEN)
    f.node('c4','c4',(568,259,171,79),'FastAPI 进程\nAgent / RAG',20,color=PURPLE)
    f.node('c3','c3',(850,94,210,113),'db\nPostgreSQL\npgvector',23,True,color=ORANGE)
    f.node('redis','c3',(850,267,210,97),'redis\n临时状态与缓存',21,color=ORANGE)
    f.node('cloud','cloud',(230,447,640,65),'外部模型服务：视觉、文本与嵌入接口',23,True,color='#922B21')
    f.edge('e12','c1','c2-i1',[(245,181),(372,181)]);f.label('proxy','/api 代理',245,139,115,BLUE,19)
    f.edge('task-start','c2-i1','task',[(458,204),(458,259)],GREEN);f.label('start-label','启动',382,218,64,GREEN,18)
    f.edge('api-start','c2-i1','c4',[(653,204),(653,259)],PURPLE);f.label('api-label','HTTP',660,218,77,PURPLE,18)
    f.edge('e23','c2-i1','c3',[(739,176),(803,176),(803,150),(850,150)],ORANGE)
    f.edge('redis-link','c2-i1','redis',[(739,183),(795,183),(795,314),(850,314)],ORANGE)
    f.edge('e43','c4','c3',[(739,287),(780,287),(780,222),(960,222),(960,207)],ORANGE)
    f.edge('vision-api','task','cloud',[(458,338),(458,447)],'#C0392B')
    f.edge('agent-api','c4','cloud',[(653,338),(653,447)],'#C0392B')
    f.label('boundary','辅助进程不另计服务',680,374,386,BLUE,20)
    f.save()

def mapping():
    f=Figure(1,'fig4-1',520)
    f.node('n1','n1',(300,10,500,59),'一行 OCR 网格：表头与字段值',24,True,color=BLUE)
    f.node('zone1','zone1',(30,108,1040,244),bg=True,fill='#F8FBFD',stroke='#A8C6D8')
    f.node('n3-ocr','n3-ocr',(60,133,440,65),'表头适配（后计算）',25,True,color=GREEN)
    f.node('header-detail','n3-ocr',(60,220,440,108),'精确匹配 → 去普通空格\n→ 包含匹配 → 编辑距离\n候选来自元数据字段配置',21,color=GREEN,fill='#FFFFFF')
    f.node('n3-mllm','n3-mllm',(600,133,440,65),'值特征校正（先计算）',25,True,color=ORANGE)
    f.node('value-detail','n3-mllm',(600,220,440,108),'启发式评分 → 选最高正分配对\n锁定该值与字段 → 继续选择\n每个值与字段至多使用一次',21,color=ORANGE,fill='#FFFFFF')
    f.node('n4','n4',(215,394,670,63),'先放表头映射结果，再以校正结果覆盖同名字段',22,True,color=BLUE)
    f.edge('table-path','n1','n3-ocr',[(550,69),(550,89),(280,89),(280,133)],GREEN)
    f.edge('value-path','n1','n3-mllm',[(550,69),(550,89),(820,89),(820,133)],ORANGE)
    f.edge('header-next','n3-ocr','header-detail',[(280,198),(280,220)],GREEN)
    f.edge('value-next','n3-mllm','value-detail',[(820,198),(820,220)],ORANGE)
    f.edge('header-merge','header-detail','n4',[(280,328),(280,372),(550,372),(550,394)],GREEN)
    f.edge('value-merge','value-detail','n4',[(820,328),(820,372),(550,372),(550,394)],ORANGE)
    f.label('scope','两项均在 OCR 链执行；MLLM 路径不重复执行这套 Java 字段适配。',45,477,1010,INK,20)
    f.save()

def rag():
    f=Figure(8,'fig4-2',620)
    f.node('row1-zone','row1-zone',(20,10,1060,220),bg=True)
    f.node('row1-badge','row1-badge',(40,22,205,36),'知识文档入库',23,True)
    f.node('row1-sub','row1-sub',(275,24,760,30),'独立资料入口，不自动同步结构化名册',22,color=PURPLE)
    specs=[('a1',40,180,'文档解析\n提取文本',22),('a3',255,245,'字符分块\n500 字符 / 重叠 50',21),('a4',535,230,'embedding-3\n1024 维向量',22),('store',805,250,'knowledge_chunks\n文本块与向量',21)]
    for ident,x,w,txt,size in specs:f.node(ident,ident,(x,90,w,105),txt,size,color=PURPLE if ident!='a4' else '#922B21',shape='cylinder' if ident=='store' else None)
    for j,(s,t,x1,x2) in enumerate([('a1','a3',220,255),('a3','a4',500,535),('a4','store',765,805)]):f.edge('ingest'+str(j),s,t,[(x1,143),(x2,143)],PURPLE)
    f.node('row2-zone','row2-zone',(20,281,1060,321),bg=True)
    f.node('row2-badge','row2-badge',(40,293,205,36),'提问与回答',23,True)
    f.node('row2-sub','row2-sub',(427,295,620,30),'限定 ready 资料，按余弦距离排序',21,color=BLUE)
    for ident,x,w,txt in [('b1',40,180,'用户问题\n生成查询向量'),('b2',255,245,'检索前 3 个文本块\n聊天 top_k = 3'),('b3',535,230,'资料标题与片段\n拼接到用户问题'),('b4',805,250,'LangChain Agent\n生成完整答案')]:f.node(ident,ident,(x,359,w,88),txt,21,color=BLUE)
    for j,(s,t,x1,x2) in enumerate([('b1','b2',220,255),('b2','b3',500,535),('b3','b4',765,805)]):f.edge('answer'+str(j),s,t,[(x1,403),(x2,403)])
    f.edge('esb2','store','b2',[(930,195),(930,253),(377,253),(377,359)],ORANGE,True)
    f.label('retrieve-note','精确检索',150,243,180,ORANGE,20)
    f.node('tools','out',(40,497,550,79),'按需调用：20 个数据库工具 + 2 个联网工具\n知识检索在 Agent 之前完成',20,color=GREEN)
    f.node('out','out',(710,500,345,74),'SSE 过程状态\n最终完整答案一次发送',21,color=GREEN)
    f.edge('tools-call','b4','tools',[(855,447),(855,473),(480,473),(480,497)],GREEN)
    f.edge('answer-out','b4','out',[(990,447),(990,500)],GREEN)
    f.save()

def layout():
    f=Figure(7,'fig4-3',580)
    # Preserve the original header/sidebar/tabs/content visual hierarchy, without claiming a screenshot.
    template=next(k for k in f.original if k=='header') if 'header' in f.original else 'hdr-bc'
    f.node('header',template,(20,10,1060,58),'ArchiveBridge     当前页面 / 业务入口                              用户菜单',22,True,shape='round',fill='#EBF5FB',stroke=BLUE,color=BLUE)
    f.node('sidebar','mi1',(20,87,210,405),bg=True,fill=BLUE,stroke=BLUE)
    f.node('sb-brand','sb-brand',(35,99,180,41),'数智档桥',25,True,color='#FFFFFF',shape='text')
    for i,txt in enumerate(['仪表盘','档案上传与任务','数据管理','图表分析','AI 助手 / 知识库','系统管理'],1):f.node('mi'+str(i),'mi'+str(i),(35,151+(i-1)*51,180,39),txt,20,fill='#EBF5FB' if i==2 else BLUE,stroke='none',color=BLUE if i==2 else '#FFFFFF')
    f.node('tabbar','t2',(251,87,829,55),bg=True,fill='#F4F7FA',stroke='#A9BCC9')
    for i,(x,w,txt) in enumerate([(264,165,'仪表盘'),(444,175,'档案上传'),(634,175,'图表分析'),(824,240,'AI 助手')],1):f.node('t'+str(i),'t'+str(i),(x,96,w,36),txt,20,color='#FFFFFF' if i==2 else BLUE)
    f.node('content','comp-title',(251,159,829,333),bg=True,shape='round',fill='#F8FBFD',stroke='#A9BCC9')
    f.node('comp-title','comp-title',(290,205,750,48),'业务内容区：当前激活组件',27,True,color=BLUE)
    f.node('comp-sub','comp-sub',(290,281,750,81),'由 tabStore.activeTab 选择动态组件\nkeep-alive 保留组件状态',23,color=BLUE)
    f.node('ph-note','ph-note',(290,399,750,44),'界面布局示意，非系统运行截图',23,True,color=ORANGE)
    f.node('flow','fl1',(30,518,1040,43),'点击菜单 → 添加或激活内部标签 → 切换业务组件；刷新后返回默认页面',21,color=BLUE)
    f.save()

def patch_docx():
    source=PAPER/'最终版修订'/'论文_最终代码一致性修订_实验待实测.docx'
    target=HERE/'论文_原图风格修订_实验待实测.docx'
    ns={'w':'http://schemas.openxmlformats.org/wordprocessingml/2006/main','a':'http://schemas.openxmlformats.org/drawingml/2006/main','r':'http://schemas.openxmlformats.org/officeDocument/2006/relationships','wp':'http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing'}
    with ZipFile(source) as z:
        root=ET.fromstring(z.read('word/document.xml'));rels=ET.fromstring(z.read('word/_rels/document.xml.rels'))
        targets={r.get('Id'):'word/'+r.get('Target') for r in rels}
        replacements={}
        for drawing in root.findall('.//w:drawing',ns):
            alt=drawing.find('.//wp:docPr',ns)
            label=(alt.get('descr','')+' '+alt.get('name',''))
            match=re.search(r'图\s*(\d+-\d+)',label)
            if not match:continue
            fig='fig'+match.group(1);file=OUT/(fig+'.png')
            if file.exists():
                rid=drawing.find('.//a:blip',ns).get('{'+ns['r']+'}embed')
                replacements[targets[rid]]=file.read_bytes()
        assert len(replacements)==7,(len(replacements),list(replacements))
        with ZipFile(target,'w',ZIP_DEFLATED) as out:
            for info in z.infolist():out.writestr(info,replacements.get(info.filename,z.read(info.filename)))
    with ZipFile(source) as a,ZipFile(target) as b:
        altered=[n for n in a.namelist() if a.read(n)!=b.read(n)]
        assert set(altered)==set(replacements)
        assert a.read('word/document.xml')==b.read('word/document.xml')
    return {'input':source.name,'output':target.name,'source_sha256':sha(source.read_bytes()),'changed_zip_parts':altered,'body_xml_identical':True,'preserved_text_equations_tables_toc':True}

if __name__=='__main__':
    for create in [architecture,ingestion,database,deployment,mapping,rag,layout]:create()
    result=patch_docx()
    (HERE/'图源修订与保留校验.json').write_text(json.dumps({'figures':REPORT,'document':result},ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(result,ensure_ascii=False,indent=2))
