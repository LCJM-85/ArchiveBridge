package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.dto.StudentDTO;
import edu.scau.scauarchiveinsight.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @GetMapping("/page")
    public R<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String createTimeStart,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) String updateTimeStart,
            @RequestParam(required = false) String updateTimeEnd) {
        var page = studentService.page(current, size, keyword, createTimeStart, createTimeEnd, updateTimeStart, updateTimeEnd);
        Map<String, Object> data = new HashMap<>();
        data.put("records", page.getRecords());
        data.put("total", page.getTotal());
        data.put("current", page.getCurrent());
        data.put("size", page.getSize());
        data.put("pages", page.getPages());
        return R.ok(data);
    }

    @PostMapping("/add")
    public R<Void> add(@RequestBody StudentDTO dto) {
        studentService.add(dto);
        return R.ok(null, "添加成功");
    }

    @PutMapping("/update")
    public R<Void> update(@RequestBody StudentDTO dto) {
        studentService.update(dto);
        return R.ok(null, "更新成功");
    }

    @DeleteMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id) {
        studentService.delete(id);
        return R.ok(null, "删除成功");
    }

    @GetMapping("/provinces")
    public R<?> provinces() {
        return R.ok(studentService.listProvinces());
    }

    @GetMapping("/majors")
    public R<?> majors() {
        return R.ok(studentService.listMajors());
    }

    @GetMapping("/classes")
    public R<?> classes() {
        return R.ok(studentService.listClasses());
    }
}
