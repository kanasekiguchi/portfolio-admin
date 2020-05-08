package com.seattleacademy.team20;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

@Controller
public class SkillController {
	private static final Logger logger = LoggerFactory.getLogger(SkillController.class);

	@RequestMapping(value = "/skillUpload", method = RequestMethod.GET)
	public String skillUpload(Locale locale, Model model) throws IOException {
		logger.info("welcome SkillUpload! The client local is {}", locale);
		Date date = new Date();
		DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, locale);
		String formattedDate = dateFormat.format(date);
		model.addAttribute("severTime", formattedDate);
		initialize();
		List<Skill> skills = selectSkills();
		uploadSkill(skills);
		return "skillUpload";
	}

	//タスク１０
	//これはよくわからない
	@Autowired
	//privateはこのページでしか使えない（反映できない）宣言 jdbcTamplateにjdbcTamplateっていう名前をつけている
	private JdbcTemplate jdbcTamplate;

	//listの宣言
	//publicはどこのページにも使える(反映できる)宣言

	public List<Skill> selectSkills() {
		final String sql = "select * from skills";
		return jdbcTamplate.query(sql, new RowMapper<Skill>() {
			public Skill mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new Skill(rs.getInt("id"), rs.getString("category"), rs.getString("name"), rs.getInt("score"));

			}
		});
	}

	private FirebaseApp app;

	// SDKの初期化
	public void initialize() throws IOException {
		FileInputStream refreshToken = new FileInputStream(
				"/Users/sekiguchikana/key/devportfolio-62aff-firebase-adminsdk-q01pt-83a9fbf3e6.json");
		FirebaseOptions options = new FirebaseOptions.Builder()
				.setCredentials(GoogleCredentials.fromStream(refreshToken))
				.setDatabaseUrl("https://devportfolio-62aff.firebaseio.com/")
				.build();
		app = FirebaseApp.initializeApp(options, "other");
	}

	public void uploadSkill(List<Skill> skills) {
		//データの保存
		final FirebaseDatabase database = FirebaseDatabase.getInstance(app);
		DatabaseReference ref = database.getReference("skills");

		//	Map型リストの作成。そのままだと、型が合わないので、ここで型を合わせてFirebaseに合わせる。
		//	Stringで聞かれたものに対し、Object型で返す。
		//	全てのJavaクラスはObject型を継承しているよ！→何でも屋さんみたいな。Objectは一番強い。
		List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
		Map<String, Object> map;
		Map<String, List<Skill>> skillMap = skills.stream().collect(Collectors.groupingBy(Skill::getCategory));
		for (Map.Entry<String, List<Skill>> entry : skillMap.entrySet()) {
			//			System.out.println(entry.getKey());
			//			System.out.println(entrygetValue());
			map = new HashMap<>();
			map.put("category", entry.getKey());
			map.put("skill", entry.getValue());
			//			dataMap = new HashMap<>();
			//			dataMap.put("id", category.getCategory());
			//			dataMap.put("name",category.getName());
			//			dataMap.put("score",category.getScore());
			//			dataMap.put("skills",category.stream()
			//					.collect(Collectors.toList()));
			dataList.add(map);
		}
		//リアルタイムデータベースの更新
		ref.setValue(dataList, new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(DatabaseError databaseError, DatabaseReference databeseReference) {
				if (databaseError != null) {
					System.out.println("Data could be saved" + databaseError.getMessage());
				} else {
					System.out.println("Data save successfully.");
				}
			}

		});
	}
}