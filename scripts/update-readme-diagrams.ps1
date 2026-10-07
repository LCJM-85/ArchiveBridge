# 在原 draw.io SVG 上做增量修改，保留原形状、连线、配色及可编辑绘图数据。
# 输出审查版 SVG/XML；不覆盖原图。SVG 内嵌 mxfile 是实际显示布局的来源。
$ErrorActionPreference = 'Stop'
$imageDir = Join-Path (Split-Path $PSScriptRoot -Parent) 'docs/images'
$svgNs = 'http://www.w3.org/2000/svg'
$htmlNs = 'http://www.w3.org/1999/xhtml'
$utf8 = [System.Text.UTF8Encoding]::new($false)

function Cell($id) { return $script:graph.SelectSingleNode("//mxCell[@id='$id']") }
function SvgCell($id) { return $script:svg.SelectSingleNode("//*[@data-cell-id='$id']") }
function Set-Label($id, $html, $height = 0, $width = 0) {
    $cell = Cell $id
    $cell.SetAttribute('value', $html)
    $geo = $cell.SelectSingleNode('mxGeometry')
    if ($height) { $geo.SetAttribute('height', [string]$height) }
    if ($width) { $geo.SetAttribute('width', [string]$width) }
    $group = SvgCell $id
    $label = $group.SelectSingleNode(".//*[local-name()='div' and contains(@style,'display: inline-block')]")
    if ($label) {
        $label.InnerXml = $html
        if ($width) {
            $style = $label.GetAttribute('style') -replace 'white-space: nowrap', 'white-space: normal'
            $style = $style -replace 'width: [^;]+;', ''
            $label.SetAttribute('style', "$style width: $($width - 2)px; overflow-wrap: anywhere;")
        }
        # 原描述文字多为顶部对齐；多行描述改为同一行框内垂直居中。
        if ($height) {
            $container = $label.ParentNode.ParentNode
            $y = [double]$geo.GetAttribute('y') + $script:offsetY + [double]$geo.GetAttribute('height') / 2
            $style = $container.GetAttribute('style') -replace 'align-items: unsafe flex-start', 'align-items: unsafe center'
            $style = $style -replace 'padding-top: [^;]+;', "padding-top: ${y}px;"
            $container.SetAttribute('style', $style)
        }
    }
    $fallback = $group.SelectSingleNode(".//*[local-name()='text']")
    if ($fallback) { $fallback.InnerText = [System.Net.WebUtility]::HtmlDecode(($html -replace '<[^>]+>', ' ')) }
}

function Add-Node($id, $label, $x, $y, $width, $height, $fill, $stroke, $color, $size = 10) {
    $cell = $script:graph.CreateElement('mxCell')
    foreach ($entry in @{id=$id;parent='1';vertex='1';value=($label.Replace("`n", '<br/>'));style="rounded=1;whiteSpace=wrap;html=1;arcSize=35;fillColor=$fill;strokeColor=$stroke;fontColor=$color;fontFamily=Microsoft YaHei;fontSize=$size;align=center;verticalAlign=middle;"}.GetEnumerator()) {
        $cell.SetAttribute($entry.Key, [string]$entry.Value)
    }
    $geo = $script:graph.CreateElement('mxGeometry')
    foreach ($entry in @{x=$x;y=$y;width=$width;height=$height;'as'='geometry'}.GetEnumerator()) { $geo.SetAttribute($entry.Key, [string]$entry.Value) }
    [void]$cell.AppendChild($geo)
    [void]$script:graph.SelectSingleNode('//root').AppendChild($cell)
    $g = $script:svg.CreateElement('g', $svgNs)
    $g.SetAttribute('data-cell-id', $id)
    $rect = $script:svg.CreateElement('rect', $svgNs)
    foreach ($entry in @{x=($x+$script:offsetX);y=($y+$script:offsetY);width=$width;height=$height;rx=6;fill=$fill;stroke=$stroke;'stroke-width'=1}.GetEnumerator()) { $rect.SetAttribute($entry.Key, [string]$entry.Value) }
    [void]$g.AppendChild($rect)
    $text = $script:svg.CreateElement('text', $svgNs)
    foreach ($entry in @{x=($x+$script:offsetX+$width/2);y=($y+$script:offsetY+$height/2+3);fill=$color;'text-anchor'='middle';'font-family'="Microsoft YaHei, sans-serif";'font-size'=$size}.GetEnumerator()) { $text.SetAttribute($entry.Key, [string]$entry.Value) }
    $lines = $label.Split("`n")
    if ($lines.Count -eq 1) { $text.InnerText = $label } else {
        for ($i = 0; $i -lt $lines.Count; $i++) {
            $span = $script:svg.CreateElement('tspan', $svgNs)
            $span.SetAttribute('x', $text.GetAttribute('x'))
            $span.SetAttribute('y', [string]($y+$script:offsetY+$height/2+3+($i-($lines.Count-1)/2)*12))
            $span.InnerText = $lines[$i]
            [void]$text.AppendChild($span)
        }
    }
    [void]$g.AppendChild($text)
    [void]$script:drawing.AppendChild($g)
}

function Add-Line($id, $x1, $y1, $x2, $y2, $color) {
    $cell = $script:graph.CreateElement('mxCell')
    foreach ($entry in @{id=$id;parent='1';edge='1';style="endArrow=none;html=1;strokeColor=$color;strokeWidth=1.5;"}.GetEnumerator()) { $cell.SetAttribute($entry.Key, [string]$entry.Value) }
    $geo = $script:graph.CreateElement('mxGeometry'); $geo.SetAttribute('relative','1'); $geo.SetAttribute('as','geometry')
    foreach ($point in @(@($x1,$y1,'sourcePoint'),@($x2,$y2,'targetPoint'))) {
        $p=$script:graph.CreateElement('mxPoint'); $p.SetAttribute('x',[string]$point[0]); $p.SetAttribute('y',[string]$point[1]); $p.SetAttribute('as',$point[2]); [void]$geo.AppendChild($p)
    }
    [void]$cell.AppendChild($geo); [void]$script:graph.SelectSingleNode('//root').AppendChild($cell)
    $g=$script:svg.CreateElement('g',$svgNs); $g.SetAttribute('data-cell-id',$id)
    $path=$script:svg.CreateElement('path',$svgNs)
    $path.SetAttribute('d',"M $($x1+$script:offsetX) $($y1+$script:offsetY) L $($x2+$script:offsetX) $($y2+$script:offsetY)")
    $path.SetAttribute('stroke',$color); $path.SetAttribute('fill','none'); [void]$g.AppendChild($path); [void]$script:drawing.AppendChild($g)
}

foreach ($name in @('系统架构图','项目目录结构图')) {
    $script:svg = [xml](Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $imageDir "$name.svg"))
    $script:graph = [xml]$script:svg.DocumentElement.GetAttribute('content')
    $referenceId = if ($name -eq '系统架构图') { 'layer-fe' } else { 'n-be' }
    $reference = Cell $referenceId
    $referenceSvg = SvgCell $referenceId
    $rect = $referenceSvg.SelectSingleNode(".//*[local-name()='rect']")
    $script:offsetX = [double]$rect.GetAttribute('x') - [double]$reference.mxGeometry.x
    $script:offsetY = [double]$rect.GetAttribute('y') - [double]$reference.mxGeometry.y
    $script:drawing = $referenceSvg.ParentNode

    if ($name -eq '系统架构图') {
        Set-Label 'fe-note' '入库审查 · 原图分屏 · 记录修正 · JWT 路由守卫'
        Set-Label 'be-flow' '<b>Controller</b> (REST)<br/><font style="font-size:10px;color:#117A65;">→ Service → Processor</font>'
        Set-Label 'be-proc' '<b>Processor 管道</b><br/><font style="font-size:10px;">CSV · Excel · PDF · Image · LLM</font><br/><font style="font-size:9px;">结构化 → 审查草稿 → 确认入库</font>'
        Set-Label 'db-ext3' '事实 / 维度 / 草稿'
        $old = (Cell 'py-ai').GetAttribute('value')
        Set-Label 'py-ai' ($old.Replace('19 工具','22 工具'))
        Set-Label 'py-note' '后端容器内：AI 服务 / 计算脚本子进程'
        Set-Label 'llm-cloud' '<font style="font-size:13px;color:#922B21;"><b>LLM API</b></font><div><font style="font-size:10px;color:#A93226;">智谱 GLM 系列</font></div><div><font style="font-size:10px;color:#CB4335;">可配置兼容视觉接口</font></div>'
        Add-Node 'be-review-note' '审查修正 / 校验 → 确认后事务入库' 796 336 328 24 '#F0FAF7' '#52BE80' '#1E8449'
        Add-Node 'db-review-draft' 'archive_review_draft · 入库前审查' 70 646 260 26 '#F2F4F4' '#85929E' '#2C3E50'
    } else {
        Set-Label 'n-sql-desc' '后端 sql/ 初始化<br/>schema.sql 含审查表' 32 210
        Set-Label 'j-ctrl-d' 'REST API / 入库审查<br/><font style="font-size:9px;">ReviewDraftController.java</font>' 36 260
        (Cell 'j-svc-d').mxGeometry.SetAttribute('y','394')
        Set-Label 'j-svc-d' '核心业务 / 审查确认<br/><font style="font-size:8px;">ReviewDraftService.java<br/>SourceFileNameService.java</font>' 36 129
        Set-Label 'j-proc-d' '文件解析 → 审查草稿'
        Set-Label 'p1d' 'LangChain / RAG / 年份约束'
        Set-Label 'p2d' 'OCR 表格识别 / LLM 提取'
        Set-Label 'fv1d' '页面组件 / 入库审查<br/><font style="font-size:9px;">review/ArchiveReview.vue</font>' 36 230
        Set-Label 'fv2d' '接口封装 / review.js'
        Set-Label 'fv3d' 'Pinia 状态 / review.js'
        Set-Label 'n-st-desc' '后端 storage/ 原文件' 20 225
        Set-Label 'n-md-desc' '后端 models/ 缓存' 20 225
        # 前端仅增加一行组件；后续存储/模型节点整体向下平移 48px。
        $shiftIds = @('st-v','st-h','n-st','n-st-desc','n-st-tag','md-v','md-h','n-md','n-md-desc','n-md-tag','root-end','CbOenYWplSs6vJ4xgEg3-16','CbOenYWplSs6vJ4xgEg3-17')
        foreach ($id in $shiftIds) {
            $cell = Cell $id
            foreach ($node in $cell.SelectNodes('.//*[@y]')) { $node.SetAttribute('y', [string]([double]$node.GetAttribute('y')+48)) }
            $g = SvgCell $id
            $g.SetAttribute('transform', 'translate(0,48)')
        }
        (Cell 'fe-box').mxGeometry.SetAttribute('height','244')
        (SvgCell 'fe-box').SelectSingleNode(".//*[local-name()='rect']").SetAttribute('height','244')
        $geo = (Cell 'fe-spine').mxGeometry
        foreach ($node in $geo.SelectNodes('.//*[@y]')) { if ([double]$node.GetAttribute('y') -ge 1082) { $node.SetAttribute('y',[string]([double]$node.GetAttribute('y')+48)) } }
        $spine = (SvgCell 'fe-spine').SelectSingleNode(".//*[local-name()='path']")
        $spine.SetAttribute('d', ($spine.GetAttribute('d') -replace '951\.4$', '999.4'))
        foreach ($id in @('n-st-tag','n-md-tag')) {
            $geo = (Cell $id).mxGeometry
            $dx = 490 - [double]$geo.x
            $geo.SetAttribute('x','490')
            (SvgCell $id).SetAttribute('transform',"translate($dx,48)")
        }
        Add-Node 'fv5' 'components/' 144 1092 120 36 '#FFFFFF' '#FDCB6E' '#E17055' 11
        Add-Node 'fv5d' "SourceImagePreview.vue`n原图缩放" 280 1092 148 36 'none' 'none' '#636E72' 9
        Add-Line 'fv5l' 120 1110 144 1110 '#FDCB6E'
        Add-Line 'fv5v' 120 1062 120 1110 '#FFEAA7'
        $height = [double]($script:svg.DocumentElement.GetAttribute('height') -replace 'px','') + 48
        $script:svg.DocumentElement.SetAttribute('height',"${height}px")
        $viewBox=$script:svg.DocumentElement.GetAttribute('viewBox').Split(' '); $viewBox[3]=[string]$height
        $script:svg.DocumentElement.SetAttribute('viewBox',($viewBox -join ' '))
        $background = $script:svg.DocumentElement.SelectSingleNode("./*[local-name()='rect']")
        if ($background) { $background.SetAttribute('height', [string]$height) }
    }
    $script:svg.DocumentElement.SetAttribute('content',$script:graph.OuterXml)
    [System.IO.File]::WriteAllText((Join-Path $imageDir "$name-审查版.xml"),$script:graph.OuterXml,$utf8)
    [System.IO.File]::WriteAllText((Join-Path $imageDir "$name-审查版.svg"),$script:svg.OuterXml,$utf8)
    Write-Output "Updated original-style diagram: $name"
}
