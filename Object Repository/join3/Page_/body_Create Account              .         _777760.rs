<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description></description>
   <name>body_Create Account              .         _777760</name>
   <tag></tag>
   <elementGuidId>521e536e-78e7-453f-9d9a-e9ce7d3bdfe0</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>//body</value>
      </entry>
      <entry>
         <key>CSS</key>
         <value></value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>true</useRalativeImagePath>
   <webElementProperties>
      <isSelected>false</isSelected>
      <matchCondition>equals</matchCondition>
      <name>tag</name>
      <type>Main</type>
      <value>body</value>
      <webElementGuid>59a0dd78-b894-481f-9164-3ba7b9639a06</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>false</isSelected>
      <matchCondition>equals</matchCondition>
      <name>class</name>
      <type>Main</type>
      <value>bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 min-h-screen py-10 px-4 sm:px-6 lg:px-8 flex items-center justify-center</value>
      <webElementGuid>7391ffd8-9a8d-492b-be98-a8bdcee9cc9f</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>text</name>
      <type>Main</type>
      <value>

    
        
            Create Account
            회원가입 정보를 입력해주세요.
        

        
            
            
            
                아이디 *
                
                    
                    
                        중복 확인
                    
                
                
            

            
            
                
                    비밀번호 *
                    
                
                
                    비밀번호 확인 *
                    
                
            

            
            
                이름 *
                
            

            
            
                성별 *
                
                    
                        
                        남성
                    
                    
                        
                        여성
                    
                    
                        
                        선택안함
                    
                
            

            
            
                프로필 이미지 업로드
                
                    
                    
                        
                        이미지를 선택하거나 드래그하세요
                    
                    
                        
                        새 텍스트 문서.txt
                        
                            
                        
                    
                
            

            
            
                주소 *
                
                    
                    
                        주소 검색
                    
                
                
                
                
                
                
            

            
            
                
                    통신사
                    
                        선택
                        SKT
                        KT
                        LGU+
                        알뜰폰
                    
                
                
                    이메일 도메인
                    
                        선택
                        naver.com
                        gmail.com
                        daum.net
                    
                
                
                    직업
                    
                        선택
                        학생
                        직장인
                        프리랜서
                    
                
            

            
            
                
                    
                    전체 약관에 동의합니다.
                
                
                    
                        
                        [필수] 서비스 이용약관 동의
                    
                    
                        
                        [필수] 개인정보 수집 및 이용 동의
                    
                
            

            
                가입하기
            
        
    

    
    
        
        
            
                
                알림
                
            
            확인
        
    

    
    
    
        // 카카오 우편번호 검색 함수
        function sample4_execDaumPostcode() {
            new kakao.Postcode({
                oncomplete: function(data) {
                    // 도로명 주소의 노출 규칙에 따라 주소를 표시한다.
                    var roadAddr = data.roadAddress;
                    var extraRoadAddr = '';

                    // 법정동명이 있을 경우 추가한다. (법정리는 제외)
                    if(data.bname !== '' &amp;&amp; /[동|로|가]$/g.test(data.bname)){
                        extraRoadAddr += data.bname;
                    }
                    // 건물명이 있고, 공동주택일 경우 추가한다.
                    if(data.buildingName !== '' &amp;&amp; data.apartment === 'Y'){
                        extraRoadAddr += (extraRoadAddr !== '' ? ', ' + data.buildingName : data.buildingName);
                    }
                    // 표시할 참고항목이 있을 경우, 괄호까지 추가한 최종 문자열을 만든다.
                    if(extraRoadAddr !== ''){
                        extraRoadAddr = ' (' + extraRoadAddr + ')';
                    }

                    // 우편번호와 주소 정보를 해당 필드에 넣는다.
                    document.getElementById('sample4_postcode').value = data.zonecode;
                    document.getElementById('sample4_roadAddress').value = roadAddr;
                    document.getElementById('sample4_jibunAddress').value = data.jibunAddress;
                    
                    // 참고항목 문자열이 있을 경우 해당 필드에 넣는다.
                    if(roadAddr !== ''){
                        document.getElementById('sample4_extraAddress').value = extraRoadAddr;
                    } else {
                        document.getElementById('sample4_extraAddress').value = '';
                    }

                    var guideTextBox = document.getElementById('guide');
                    // 사용자가 '선택 안함'을 클릭한 경우, 예상 주소라는 표시를 해준다.
                    if(data.autoRoadAddress) {
                        var expRoadAddr = data.autoRoadAddress + extraRoadAddr;
                        guideTextBox.innerHTML = '(예상 도로명 주소 : ' + expRoadAddr + ')';
                        guideTextBox.style.display = 'block';
                    } else if(data.autoJibunAddress) {
                        var expJibunAddr = data.autoJibunAddress;
                        guideTextBox.innerHTML = '(예상 지번 주소 : ' + expJibunAddr + ')';
                        guideTextBox.style.display = 'block';
                    } else {
                        guideTextBox.innerHTML = '';
                        guideTextBox.style.display = 'none';
                    }

                    // 상세주소 입력란으로 포커스 이동
                    document.getElementById('sample4_detailAddress').focus();
                }
            }).open();
        }

        function checkId() {
            var id = document.getElementById('userId').value;
            if (!id) return showModal('알림', '아이디를 입력하세요.', 'error');
            document.getElementById('isIdChecked').value = &quot;true&quot;;
            showModal('확인 완료', '사용 가능한 아이디입니다.', 'success');
        }

        function handleFileSelect(input) {
            var file = input.files[0];
            if (file) {
                document.getElementById('fileName').textContent = file.name;
                document.getElementById('filePlaceholder').classList.add('hidden');
                document.getElementById('fileInfo').classList.remove('hidden');
                document.getElementById('fileInfo').classList.add('flex');
            }
        }

        function resetFile(e) {
            e.stopPropagation();
            document.getElementById('profileImg').value = &quot;&quot;;
            document.getElementById('filePlaceholder').classList.remove('hidden');
            document.getElementById('fileInfo').classList.remove('flex');
            document.getElementById('fileInfo').classList.add('hidden');
        }

        function toggleAll() {
            var isChecked = document.getElementById('checkAll').checked;
            var checks = document.querySelectorAll('.term-check');
            for (var i = 0; i &lt; checks.length; i++) {
                checks[i].checked = isChecked;
            }
        }

        function submitForm() {
            if (document.getElementById('isIdChecked').value !== &quot;true&quot;) {
                return showModal('실패', '중복 확인이 필요합니다.', 'error');
            }
            showModal('축하합니다', '회원가입이 완료되었습니다!', 'success');
        }

        function showModal(title, msg, type) {
            var m = document.getElementById('customModal');
            document.getElementById('modalTitle').textContent = title;
            document.getElementById('modalMessage').textContent = msg;
            var icon = document.getElementById('modalIcon');
            icon.innerHTML = type === 'success' ? '&lt;i class=&quot;fas fa-check&quot;>&lt;/i>' : '&lt;i class=&quot;fas fa-times&quot;>&lt;/i>';
            icon.className = 'mx-auto flex items-center justify-center h-12 w-12 rounded-full mb-4 ' + (type === 'success' ? 'bg-green-100 text-green-500' : 'bg-red-100 text-red-500');
            m.classList.remove('hidden');
        }

        function closeModal() {
            document.getElementById('customModal').classList.add('hidden');
        }
    

/html[1]/body[@class=&quot;bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 min-h-screen py-10 px-4 sm:px-6 lg:px-8 flex items-center justify-center&quot;]</value>
      <webElementGuid>e51b767d-1750-49e6-b96c-294e6f408d82</webElementGuid>
   </webElementProperties>
   <webElementProperties>
      <isSelected>false</isSelected>
      <matchCondition>equals</matchCondition>
      <name>xpath</name>
      <type>Main</type>
      <value>/html[1]/body[@class=&quot;bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 min-h-screen py-10 px-4 sm:px-6 lg:px-8 flex items-center justify-center&quot;]</value>
      <webElementGuid>c0c23a10-bab2-4c8e-b11e-b6caba39618e</webElementGuid>
   </webElementProperties>
   <webElementXpaths>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>xpath:position</name>
      <type>Main</type>
      <value>//body</value>
      <webElementGuid>e7c3cf4d-374a-4627-a535-b0583956a2cb</webElementGuid>
   </webElementXpaths>
   <webElementXpaths>
      <isSelected>false</isSelected>
      <matchCondition>equals</matchCondition>
      <name>xpath:customAttributes</name>
      <type>Main</type>
      <value>//body[(text() = concat(&quot;

    
        
            Create Account
            회원가입 정보를 입력해주세요.
        

        
            
            
            
                아이디 *
                
                    
                    
                        중복 확인
                    
                
                
            

            
            
                
                    비밀번호 *
                    
                
                
                    비밀번호 확인 *
                    
                
            

            
            
                이름 *
                
            

            
            
                성별 *
                
                    
                        
                        남성
                    
                    
                        
                        여성
                    
                    
                        
                        선택안함
                    
                
            

            
            
                프로필 이미지 업로드
                
                    
                    
                        
                        이미지를 선택하거나 드래그하세요
                    
                    
                        
                        새 텍스트 문서.txt
                        
                            
                        
                    
                
            

            
            
                주소 *
                
                    
                    
                        주소 검색
                    
                
                
                
                
                
                
            

            
            
                
                    통신사
                    
                        선택
                        SKT
                        KT
                        LGU+
                        알뜰폰
                    
                
                
                    이메일 도메인
                    
                        선택
                        naver.com
                        gmail.com
                        daum.net
                    
                
                
                    직업
                    
                        선택
                        학생
                        직장인
                        프리랜서
                    
                
            

            
            
                
                    
                    전체 약관에 동의합니다.
                
                
                    
                        
                        [필수] 서비스 이용약관 동의
                    
                    
                        
                        [필수] 개인정보 수집 및 이용 동의
                    
                
            

            
                가입하기
            
        
    

    
    
        
        
            
                
                알림
                
            
            확인
        
    

    
    
    
        // 카카오 우편번호 검색 함수
        function sample4_execDaumPostcode() {
            new kakao.Postcode({
                oncomplete: function(data) {
                    // 도로명 주소의 노출 규칙에 따라 주소를 표시한다.
                    var roadAddr = data.roadAddress;
                    var extraRoadAddr = &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot;;

                    // 법정동명이 있을 경우 추가한다. (법정리는 제외)
                    if(data.bname !== &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot; &amp;&amp; /[동|로|가]$/g.test(data.bname)){
                        extraRoadAddr += data.bname;
                    }
                    // 건물명이 있고, 공동주택일 경우 추가한다.
                    if(data.buildingName !== &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot; &amp;&amp; data.apartment === &quot; , &quot;'&quot; , &quot;Y&quot; , &quot;'&quot; , &quot;){
                        extraRoadAddr += (extraRoadAddr !== &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot; ? &quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot; + data.buildingName : data.buildingName);
                    }
                    // 표시할 참고항목이 있을 경우, 괄호까지 추가한 최종 문자열을 만든다.
                    if(extraRoadAddr !== &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot;){
                        extraRoadAddr = &quot; , &quot;'&quot; , &quot; (&quot; , &quot;'&quot; , &quot; + extraRoadAddr + &quot; , &quot;'&quot; , &quot;)&quot; , &quot;'&quot; , &quot;;
                    }

                    // 우편번호와 주소 정보를 해당 필드에 넣는다.
                    document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_postcode&quot; , &quot;'&quot; , &quot;).value = data.zonecode;
                    document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_roadAddress&quot; , &quot;'&quot; , &quot;).value = roadAddr;
                    document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_jibunAddress&quot; , &quot;'&quot; , &quot;).value = data.jibunAddress;
                    
                    // 참고항목 문자열이 있을 경우 해당 필드에 넣는다.
                    if(roadAddr !== &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot;){
                        document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_extraAddress&quot; , &quot;'&quot; , &quot;).value = extraRoadAddr;
                    } else {
                        document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_extraAddress&quot; , &quot;'&quot; , &quot;).value = &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot;;
                    }

                    var guideTextBox = document.getElementById(&quot; , &quot;'&quot; , &quot;guide&quot; , &quot;'&quot; , &quot;);
                    // 사용자가 &quot; , &quot;'&quot; , &quot;선택 안함&quot; , &quot;'&quot; , &quot;을 클릭한 경우, 예상 주소라는 표시를 해준다.
                    if(data.autoRoadAddress) {
                        var expRoadAddr = data.autoRoadAddress + extraRoadAddr;
                        guideTextBox.innerHTML = &quot; , &quot;'&quot; , &quot;(예상 도로명 주소 : &quot; , &quot;'&quot; , &quot; + expRoadAddr + &quot; , &quot;'&quot; , &quot;)&quot; , &quot;'&quot; , &quot;;
                        guideTextBox.style.display = &quot; , &quot;'&quot; , &quot;block&quot; , &quot;'&quot; , &quot;;
                    } else if(data.autoJibunAddress) {
                        var expJibunAddr = data.autoJibunAddress;
                        guideTextBox.innerHTML = &quot; , &quot;'&quot; , &quot;(예상 지번 주소 : &quot; , &quot;'&quot; , &quot; + expJibunAddr + &quot; , &quot;'&quot; , &quot;)&quot; , &quot;'&quot; , &quot;;
                        guideTextBox.style.display = &quot; , &quot;'&quot; , &quot;block&quot; , &quot;'&quot; , &quot;;
                    } else {
                        guideTextBox.innerHTML = &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot;;
                        guideTextBox.style.display = &quot; , &quot;'&quot; , &quot;none&quot; , &quot;'&quot; , &quot;;
                    }

                    // 상세주소 입력란으로 포커스 이동
                    document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_detailAddress&quot; , &quot;'&quot; , &quot;).focus();
                }
            }).open();
        }

        function checkId() {
            var id = document.getElementById(&quot; , &quot;'&quot; , &quot;userId&quot; , &quot;'&quot; , &quot;).value;
            if (!id) return showModal(&quot; , &quot;'&quot; , &quot;알림&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;아이디를 입력하세요.&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;error&quot; , &quot;'&quot; , &quot;);
            document.getElementById(&quot; , &quot;'&quot; , &quot;isIdChecked&quot; , &quot;'&quot; , &quot;).value = &quot;true&quot;;
            showModal(&quot; , &quot;'&quot; , &quot;확인 완료&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;사용 가능한 아이디입니다.&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;success&quot; , &quot;'&quot; , &quot;);
        }

        function handleFileSelect(input) {
            var file = input.files[0];
            if (file) {
                document.getElementById(&quot; , &quot;'&quot; , &quot;fileName&quot; , &quot;'&quot; , &quot;).textContent = file.name;
                document.getElementById(&quot; , &quot;'&quot; , &quot;filePlaceholder&quot; , &quot;'&quot; , &quot;).classList.add(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
                document.getElementById(&quot; , &quot;'&quot; , &quot;fileInfo&quot; , &quot;'&quot; , &quot;).classList.remove(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
                document.getElementById(&quot; , &quot;'&quot; , &quot;fileInfo&quot; , &quot;'&quot; , &quot;).classList.add(&quot; , &quot;'&quot; , &quot;flex&quot; , &quot;'&quot; , &quot;);
            }
        }

        function resetFile(e) {
            e.stopPropagation();
            document.getElementById(&quot; , &quot;'&quot; , &quot;profileImg&quot; , &quot;'&quot; , &quot;).value = &quot;&quot;;
            document.getElementById(&quot; , &quot;'&quot; , &quot;filePlaceholder&quot; , &quot;'&quot; , &quot;).classList.remove(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
            document.getElementById(&quot; , &quot;'&quot; , &quot;fileInfo&quot; , &quot;'&quot; , &quot;).classList.remove(&quot; , &quot;'&quot; , &quot;flex&quot; , &quot;'&quot; , &quot;);
            document.getElementById(&quot; , &quot;'&quot; , &quot;fileInfo&quot; , &quot;'&quot; , &quot;).classList.add(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
        }

        function toggleAll() {
            var isChecked = document.getElementById(&quot; , &quot;'&quot; , &quot;checkAll&quot; , &quot;'&quot; , &quot;).checked;
            var checks = document.querySelectorAll(&quot; , &quot;'&quot; , &quot;.term-check&quot; , &quot;'&quot; , &quot;);
            for (var i = 0; i &lt; checks.length; i++) {
                checks[i].checked = isChecked;
            }
        }

        function submitForm() {
            if (document.getElementById(&quot; , &quot;'&quot; , &quot;isIdChecked&quot; , &quot;'&quot; , &quot;).value !== &quot;true&quot;) {
                return showModal(&quot; , &quot;'&quot; , &quot;실패&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;중복 확인이 필요합니다.&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;error&quot; , &quot;'&quot; , &quot;);
            }
            showModal(&quot; , &quot;'&quot; , &quot;축하합니다&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;회원가입이 완료되었습니다!&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;success&quot; , &quot;'&quot; , &quot;);
        }

        function showModal(title, msg, type) {
            var m = document.getElementById(&quot; , &quot;'&quot; , &quot;customModal&quot; , &quot;'&quot; , &quot;);
            document.getElementById(&quot; , &quot;'&quot; , &quot;modalTitle&quot; , &quot;'&quot; , &quot;).textContent = title;
            document.getElementById(&quot; , &quot;'&quot; , &quot;modalMessage&quot; , &quot;'&quot; , &quot;).textContent = msg;
            var icon = document.getElementById(&quot; , &quot;'&quot; , &quot;modalIcon&quot; , &quot;'&quot; , &quot;);
            icon.innerHTML = type === &quot; , &quot;'&quot; , &quot;success&quot; , &quot;'&quot; , &quot; ? &quot; , &quot;'&quot; , &quot;&lt;i class=&quot;fas fa-check&quot;>&lt;/i>&quot; , &quot;'&quot; , &quot; : &quot; , &quot;'&quot; , &quot;&lt;i class=&quot;fas fa-times&quot;>&lt;/i>&quot; , &quot;'&quot; , &quot;;
            icon.className = &quot; , &quot;'&quot; , &quot;mx-auto flex items-center justify-center h-12 w-12 rounded-full mb-4 &quot; , &quot;'&quot; , &quot; + (type === &quot; , &quot;'&quot; , &quot;success&quot; , &quot;'&quot; , &quot; ? &quot; , &quot;'&quot; , &quot;bg-green-100 text-green-500&quot; , &quot;'&quot; , &quot; : &quot; , &quot;'&quot; , &quot;bg-red-100 text-red-500&quot; , &quot;'&quot; , &quot;);
            m.classList.remove(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
        }

        function closeModal() {
            document.getElementById(&quot; , &quot;'&quot; , &quot;customModal&quot; , &quot;'&quot; , &quot;).classList.add(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
        }
    

/html[1]/body[@class=&quot;bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 min-h-screen py-10 px-4 sm:px-6 lg:px-8 flex items-center justify-center&quot;]&quot;) or . = concat(&quot;

    
        
            Create Account
            회원가입 정보를 입력해주세요.
        

        
            
            
            
                아이디 *
                
                    
                    
                        중복 확인
                    
                
                
            

            
            
                
                    비밀번호 *
                    
                
                
                    비밀번호 확인 *
                    
                
            

            
            
                이름 *
                
            

            
            
                성별 *
                
                    
                        
                        남성
                    
                    
                        
                        여성
                    
                    
                        
                        선택안함
                    
                
            

            
            
                프로필 이미지 업로드
                
                    
                    
                        
                        이미지를 선택하거나 드래그하세요
                    
                    
                        
                        새 텍스트 문서.txt
                        
                            
                        
                    
                
            

            
            
                주소 *
                
                    
                    
                        주소 검색
                    
                
                
                
                
                
                
            

            
            
                
                    통신사
                    
                        선택
                        SKT
                        KT
                        LGU+
                        알뜰폰
                    
                
                
                    이메일 도메인
                    
                        선택
                        naver.com
                        gmail.com
                        daum.net
                    
                
                
                    직업
                    
                        선택
                        학생
                        직장인
                        프리랜서
                    
                
            

            
            
                
                    
                    전체 약관에 동의합니다.
                
                
                    
                        
                        [필수] 서비스 이용약관 동의
                    
                    
                        
                        [필수] 개인정보 수집 및 이용 동의
                    
                
            

            
                가입하기
            
        
    

    
    
        
        
            
                
                알림
                
            
            확인
        
    

    
    
    
        // 카카오 우편번호 검색 함수
        function sample4_execDaumPostcode() {
            new kakao.Postcode({
                oncomplete: function(data) {
                    // 도로명 주소의 노출 규칙에 따라 주소를 표시한다.
                    var roadAddr = data.roadAddress;
                    var extraRoadAddr = &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot;;

                    // 법정동명이 있을 경우 추가한다. (법정리는 제외)
                    if(data.bname !== &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot; &amp;&amp; /[동|로|가]$/g.test(data.bname)){
                        extraRoadAddr += data.bname;
                    }
                    // 건물명이 있고, 공동주택일 경우 추가한다.
                    if(data.buildingName !== &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot; &amp;&amp; data.apartment === &quot; , &quot;'&quot; , &quot;Y&quot; , &quot;'&quot; , &quot;){
                        extraRoadAddr += (extraRoadAddr !== &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot; ? &quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot; + data.buildingName : data.buildingName);
                    }
                    // 표시할 참고항목이 있을 경우, 괄호까지 추가한 최종 문자열을 만든다.
                    if(extraRoadAddr !== &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot;){
                        extraRoadAddr = &quot; , &quot;'&quot; , &quot; (&quot; , &quot;'&quot; , &quot; + extraRoadAddr + &quot; , &quot;'&quot; , &quot;)&quot; , &quot;'&quot; , &quot;;
                    }

                    // 우편번호와 주소 정보를 해당 필드에 넣는다.
                    document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_postcode&quot; , &quot;'&quot; , &quot;).value = data.zonecode;
                    document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_roadAddress&quot; , &quot;'&quot; , &quot;).value = roadAddr;
                    document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_jibunAddress&quot; , &quot;'&quot; , &quot;).value = data.jibunAddress;
                    
                    // 참고항목 문자열이 있을 경우 해당 필드에 넣는다.
                    if(roadAddr !== &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot;){
                        document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_extraAddress&quot; , &quot;'&quot; , &quot;).value = extraRoadAddr;
                    } else {
                        document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_extraAddress&quot; , &quot;'&quot; , &quot;).value = &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot;;
                    }

                    var guideTextBox = document.getElementById(&quot; , &quot;'&quot; , &quot;guide&quot; , &quot;'&quot; , &quot;);
                    // 사용자가 &quot; , &quot;'&quot; , &quot;선택 안함&quot; , &quot;'&quot; , &quot;을 클릭한 경우, 예상 주소라는 표시를 해준다.
                    if(data.autoRoadAddress) {
                        var expRoadAddr = data.autoRoadAddress + extraRoadAddr;
                        guideTextBox.innerHTML = &quot; , &quot;'&quot; , &quot;(예상 도로명 주소 : &quot; , &quot;'&quot; , &quot; + expRoadAddr + &quot; , &quot;'&quot; , &quot;)&quot; , &quot;'&quot; , &quot;;
                        guideTextBox.style.display = &quot; , &quot;'&quot; , &quot;block&quot; , &quot;'&quot; , &quot;;
                    } else if(data.autoJibunAddress) {
                        var expJibunAddr = data.autoJibunAddress;
                        guideTextBox.innerHTML = &quot; , &quot;'&quot; , &quot;(예상 지번 주소 : &quot; , &quot;'&quot; , &quot; + expJibunAddr + &quot; , &quot;'&quot; , &quot;)&quot; , &quot;'&quot; , &quot;;
                        guideTextBox.style.display = &quot; , &quot;'&quot; , &quot;block&quot; , &quot;'&quot; , &quot;;
                    } else {
                        guideTextBox.innerHTML = &quot; , &quot;'&quot; , &quot;&quot; , &quot;'&quot; , &quot;;
                        guideTextBox.style.display = &quot; , &quot;'&quot; , &quot;none&quot; , &quot;'&quot; , &quot;;
                    }

                    // 상세주소 입력란으로 포커스 이동
                    document.getElementById(&quot; , &quot;'&quot; , &quot;sample4_detailAddress&quot; , &quot;'&quot; , &quot;).focus();
                }
            }).open();
        }

        function checkId() {
            var id = document.getElementById(&quot; , &quot;'&quot; , &quot;userId&quot; , &quot;'&quot; , &quot;).value;
            if (!id) return showModal(&quot; , &quot;'&quot; , &quot;알림&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;아이디를 입력하세요.&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;error&quot; , &quot;'&quot; , &quot;);
            document.getElementById(&quot; , &quot;'&quot; , &quot;isIdChecked&quot; , &quot;'&quot; , &quot;).value = &quot;true&quot;;
            showModal(&quot; , &quot;'&quot; , &quot;확인 완료&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;사용 가능한 아이디입니다.&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;success&quot; , &quot;'&quot; , &quot;);
        }

        function handleFileSelect(input) {
            var file = input.files[0];
            if (file) {
                document.getElementById(&quot; , &quot;'&quot; , &quot;fileName&quot; , &quot;'&quot; , &quot;).textContent = file.name;
                document.getElementById(&quot; , &quot;'&quot; , &quot;filePlaceholder&quot; , &quot;'&quot; , &quot;).classList.add(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
                document.getElementById(&quot; , &quot;'&quot; , &quot;fileInfo&quot; , &quot;'&quot; , &quot;).classList.remove(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
                document.getElementById(&quot; , &quot;'&quot; , &quot;fileInfo&quot; , &quot;'&quot; , &quot;).classList.add(&quot; , &quot;'&quot; , &quot;flex&quot; , &quot;'&quot; , &quot;);
            }
        }

        function resetFile(e) {
            e.stopPropagation();
            document.getElementById(&quot; , &quot;'&quot; , &quot;profileImg&quot; , &quot;'&quot; , &quot;).value = &quot;&quot;;
            document.getElementById(&quot; , &quot;'&quot; , &quot;filePlaceholder&quot; , &quot;'&quot; , &quot;).classList.remove(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
            document.getElementById(&quot; , &quot;'&quot; , &quot;fileInfo&quot; , &quot;'&quot; , &quot;).classList.remove(&quot; , &quot;'&quot; , &quot;flex&quot; , &quot;'&quot; , &quot;);
            document.getElementById(&quot; , &quot;'&quot; , &quot;fileInfo&quot; , &quot;'&quot; , &quot;).classList.add(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
        }

        function toggleAll() {
            var isChecked = document.getElementById(&quot; , &quot;'&quot; , &quot;checkAll&quot; , &quot;'&quot; , &quot;).checked;
            var checks = document.querySelectorAll(&quot; , &quot;'&quot; , &quot;.term-check&quot; , &quot;'&quot; , &quot;);
            for (var i = 0; i &lt; checks.length; i++) {
                checks[i].checked = isChecked;
            }
        }

        function submitForm() {
            if (document.getElementById(&quot; , &quot;'&quot; , &quot;isIdChecked&quot; , &quot;'&quot; , &quot;).value !== &quot;true&quot;) {
                return showModal(&quot; , &quot;'&quot; , &quot;실패&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;중복 확인이 필요합니다.&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;error&quot; , &quot;'&quot; , &quot;);
            }
            showModal(&quot; , &quot;'&quot; , &quot;축하합니다&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;회원가입이 완료되었습니다!&quot; , &quot;'&quot; , &quot;, &quot; , &quot;'&quot; , &quot;success&quot; , &quot;'&quot; , &quot;);
        }

        function showModal(title, msg, type) {
            var m = document.getElementById(&quot; , &quot;'&quot; , &quot;customModal&quot; , &quot;'&quot; , &quot;);
            document.getElementById(&quot; , &quot;'&quot; , &quot;modalTitle&quot; , &quot;'&quot; , &quot;).textContent = title;
            document.getElementById(&quot; , &quot;'&quot; , &quot;modalMessage&quot; , &quot;'&quot; , &quot;).textContent = msg;
            var icon = document.getElementById(&quot; , &quot;'&quot; , &quot;modalIcon&quot; , &quot;'&quot; , &quot;);
            icon.innerHTML = type === &quot; , &quot;'&quot; , &quot;success&quot; , &quot;'&quot; , &quot; ? &quot; , &quot;'&quot; , &quot;&lt;i class=&quot;fas fa-check&quot;>&lt;/i>&quot; , &quot;'&quot; , &quot; : &quot; , &quot;'&quot; , &quot;&lt;i class=&quot;fas fa-times&quot;>&lt;/i>&quot; , &quot;'&quot; , &quot;;
            icon.className = &quot; , &quot;'&quot; , &quot;mx-auto flex items-center justify-center h-12 w-12 rounded-full mb-4 &quot; , &quot;'&quot; , &quot; + (type === &quot; , &quot;'&quot; , &quot;success&quot; , &quot;'&quot; , &quot; ? &quot; , &quot;'&quot; , &quot;bg-green-100 text-green-500&quot; , &quot;'&quot; , &quot; : &quot; , &quot;'&quot; , &quot;bg-red-100 text-red-500&quot; , &quot;'&quot; , &quot;);
            m.classList.remove(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
        }

        function closeModal() {
            document.getElementById(&quot; , &quot;'&quot; , &quot;customModal&quot; , &quot;'&quot; , &quot;).classList.add(&quot; , &quot;'&quot; , &quot;hidden&quot; , &quot;'&quot; , &quot;);
        }
    

/html[1]/body[@class=&quot;bg-gradient-to-br from-indigo-50 via-purple-50 to-pink-50 min-h-screen py-10 px-4 sm:px-6 lg:px-8 flex items-center justify-center&quot;]&quot;))]</value>
      <webElementGuid>ff06de09-e5ce-4ecd-a59c-e38a003441c1</webElementGuid>
   </webElementXpaths>
</WebElementEntity>
