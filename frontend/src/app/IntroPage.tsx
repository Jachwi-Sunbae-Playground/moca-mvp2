import { Link } from 'react-router-dom';
import logo from '../assets/jachwi-sunbae-logo.png';
import Icon, { type IconName } from '../components/ui/Icon';
import styles from './IntroPage.module.css';

type IntroFeature = {
  icon: IconName;
  title: string;
  description: string;
};

const steps: IntroFeature[] = [
  {
    icon: 'map',
    title: '지도나 주소로 매물 등록',
    description: '갑자기 보게 된 집도 현재 위치나 주소 검색으로 빠르게 남겨요.',
  },
  {
    icon: 'checklist',
    title: '사진·메모·체크 기록',
    description: '방문 전, 현장, 계약 전까지 필요한 단계만 골라 확인해요.',
  },
  {
    icon: 'inbox',
    title: '후보 매물을 PDF로 비교',
    description: '저장한 가격, 사진, 메모와 체크 결과를 한 번에 나란히 봐요.',
  },
];

const values: IntroFeature[] = [
  {
    icon: 'target',
    title: '3단계로 필요한 만큼만',
    description: '모든 단계를 강요하지 않아요. 관심이 커진 매물만 다음 단계로 이어가요.',
  },
  {
    icon: 'map',
    title: '주변 시설을 거리별로',
    description: '매물 주변 병원, 교통, 학교, 편의점과 중개업소를 500m·1km·2km로 확인해요.',
  },
  {
    icon: 'info',
    title: '추천 대신 확인한 사실을',
    description: '점수로 대신 결정하지 않고, 내가 남긴 기록을 판단하기 쉽게 정리해요.',
  },
];

const IntroPage = () => (
  <main className={styles.page} id="intro-top">
    <a className={styles.skipLink} href="#intro-content">
      소개 내용으로 바로가기
    </a>

    <header className={styles.topBar}>
      <a className={styles.brand} href="#intro-top" aria-label="자취선배 소개 처음으로">
        <img src={logo} alt="자취선배" />
      </a>
      <Link className={styles.topCta} to="/login">
        바로 시작
        <Icon name="arrow-right" size={16} />
      </Link>
    </header>

    <div className={styles.content} id="intro-content">
      <section className={styles.hero} aria-labelledby="intro-heading">
        <div className={styles.heroCopy}>
          <p className={styles.eyebrow}>처음 집을 구하는 사람의 기록 도구</p>
          <h1 id="intro-heading">
            집은 짧게 보지만,
            <br />
            놓친 문제는
            <br />
            매일 반복됩니다.
          </h1>
          <p className={styles.heroDescription}>
            주소·사진·메모·체크리스트를 한곳에 모으고, 여러 후보를 같은 기준으로 비교하세요.
          </p>
          <div className={styles.heroActions}>
            <Link className={styles.primaryCta} to="/login">
              닉네임으로 바로 시작
              <Icon name="arrow-right" size={18} />
            </Link>
            <a className={styles.secondaryCta} href="#how-to-use">
              1분 만에 둘러보기
            </a>
          </div>
          <p className={styles.entryNote}>Google 로그인 없이 이름이나 닉네임만 입력하면 바로 사용할 수 있어요.</p>
        </div>

        <div className={styles.preview} aria-label="자취선배 매물 기록 예시">
          <div className={styles.previewHeader}>
            <span>기록 중인 매물</span>
            <strong>신림역 원룸</strong>
            <small>관악구 신림동 · 보증금 1,000만원 / 월세 55만원</small>
          </div>
          <div className={styles.previewFacts}>
            <span>
              <Icon name="image" size={15} /> 사진 4장
            </span>
            <span>
              <Icon name="edit" size={15} /> 메모 있음
            </span>
            <span>
              <Icon name="link" size={15} /> 발견 경로
            </span>
          </div>
          <div className={styles.previewStages}>
            <div>
              <span>1</span>
              <p>
                <strong>온라인·전화</strong>
                <small>6/6 확인</small>
              </p>
              <i data-progress="complete" />
            </div>
            <div>
              <span>2</span>
              <p>
                <strong>집에서 확인</strong>
                <small>5/8 확인</small>
              </p>
              <i data-progress="partial" />
            </div>
            <div>
              <span>3</span>
              <p>
                <strong>계약 전</strong>
                <small>필요할 때 시작</small>
              </p>
              <i />
            </div>
          </div>
          <div className={styles.previewNearby}>
            <strong>주변 500m</strong>
            <span>교통 7</span>
            <span>편의점 12</span>
            <span>병원 4</span>
          </div>
        </div>
      </section>

      <section className={styles.problem} aria-labelledby="problem-heading">
        <p className={styles.sectionLabel}>집을 보고 난 뒤</p>
        <h2 id="problem-heading">기록이 흩어지면, 방마다 무엇이 달랐는지 기억하기 어려워요.</h2>
        <div className={styles.scatteredRecords} aria-label="여러 곳에 흩어진 자취방 기록">
          <span>카카오톡에 주소</span>
          <span>사진첩에 방 구조</span>
          <span>메모장에 가격과 일정</span>
        </div>
        <p>자취선배는 어느 매물 플랫폼에서 발견했든, 실제로 살아갈 사람의 기준으로 기록을 모아 줍니다.</p>
      </section>

      <section className={styles.howToUse} id="how-to-use" aria-labelledby="how-heading">
        <p className={styles.sectionLabel}>사용 방법</p>
        <h2 id="how-heading">방을 등록하고, 확인하고, 마지막에 비교하세요.</h2>
        <ol className={styles.stepList}>
          {steps.map((step, index) => (
            <li key={step.title}>
              <span className={styles.stepNumber}>{String(index + 1).padStart(2, '0')}</span>
              <span className={styles.featureIcon}>
                <Icon name={step.icon} size={22} />
              </span>
              <div>
                <h3>{step.title}</h3>
                <p>{step.description}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section className={styles.valueSection} aria-labelledby="value-heading">
        <p className={styles.sectionLabel}>자취선배가 돕는 방식</p>
        <h2 id="value-heading">더 많이 입력하게 하기보다, 필요한 순간에 확인할 수 있게.</h2>
        <div className={styles.valueGrid}>
          {values.map((value) => (
            <article key={value.title}>
              <span className={styles.featureIcon}>
                <Icon name={value.icon} size={22} />
              </span>
              <h3>{value.title}</h3>
              <p>{value.description}</p>
            </article>
          ))}
        </div>
      </section>

      <section className={styles.audience} aria-labelledby="audience-heading">
        <div>
          <p className={styles.sectionLabel}>이런 분에게 필요해요</p>
          <h2 id="audience-heading">앞으로 한 달 안에 자취방을 직접 보러 갈 예정인가요?</h2>
        </div>
        <ul>
          <li>처음 집을 구해 무엇을 봐야 할지 막막한 분</li>
          <li>여러 방의 사진과 조건이 자꾸 섞이는 분</li>
          <li>계약 전에 확인하지 못한 것이 있을까 불안한 분</li>
        </ul>
      </section>

      <section className={styles.finalCta} aria-labelledby="final-cta-heading">
        <span className={styles.finalIcon}>
          <Icon name="home" size={28} />
        </span>
        <p>다음 방을 보러 갈 때</p>
        <h2 id="final-cta-heading">자취선배와 함께 확인해 보세요.</h2>
        <p className={styles.finalDescription}>가입 절차 없이 닉네임으로 시작하고, 필요한 기록만 남기면 됩니다.</p>
        <Link className={styles.primaryCta} to="/login">
          자취선배 시작하기
          <Icon name="arrow-right" size={18} />
        </Link>
        <small>
          비밀번호 없이 만든 닉네임은 같은 이름을 입력한 사람과 기록을 공유합니다. 보호가 필요하면 시작할 때 선택
          비밀번호를 설정하세요.
        </small>
      </section>
    </div>

    <footer className={styles.footer}>
      <img src={logo} alt="" />
      <p>플랫폼에는 중립적으로, 집을 구하는 사람의 결정을 먼저.</p>
    </footer>
  </main>
);

export default IntroPage;
