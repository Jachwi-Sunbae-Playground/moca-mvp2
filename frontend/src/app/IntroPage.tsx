import { Link } from 'react-router-dom';
import logo from '../assets/jachwi-sunbae-logo.png';
import Icon, { type IconName } from '../components/ui/Icon';
import styles from './IntroPage.module.css';

type IntroFeature = {
  icon: IconName;
  title: string;
  description: string;
};

type LossSignal = {
  amount: string;
  title: string;
  description: string;
};

const steps: IntroFeature[] = [
  {
    icon: 'map',
    title: '지도나 주소로 매물 등록',
    description: '중개사가 갑자기 보여준 집도 위치와 발견 경로를 바로 남겨요.',
  },
  {
    icon: 'checklist',
    title: '돈 새는 질문부터 기록',
    description: '관리비·보증금·옵션·하자를 사진과 메모, 단계별 체크로 확인해요.',
  },
  {
    icon: 'inbox',
    title: '후보 매물 전체를 PDF로 비교',
    description: '가격만이 아니라 저장한 사진·메모·체크 결과를 한 번에 나란히 봐요.',
  },
];

const values: IntroFeature[] = [
  {
    icon: 'checklist',
    title: '계약 뒤 늦는 질문을 먼저',
    description: '방문 전·현장·계약 전, 관심이 커진 매물만 필요한 단계까지 확인해요.',
  },
  {
    icon: 'map',
    title: '생활 조건을 거리별로',
    description: '교통·편의점·병원·학교·중개업소를 500m·1km·2km 범위로 확인해요.',
  },
  {
    icon: 'inbox',
    title: '추천 대신 모든 기록을',
    description: '점수로 대신 결정하지 않고, 여러 방의 기록을 비교 PDF로 그대로 정리해요.',
  },
];

const lossSignals: LossSignal[] = [
  {
    amount: '2년 120만원',
    title: '매달 5만원의 별도 비용',
    description: '관리비 밖 비용은 월세 할인을 그대로 지워버릴 수 있어요.',
  },
  {
    amount: '보증금 전액',
    title: '사진에 보이지 않는 권리관계',
    description: '등기부의 소유자·근저당·압류는 계약 전에 직접 확인해야 해요.',
  },
  {
    amount: '첫 장마 이후',
    title: '새 벽지 뒤 누수와 곰팡이',
    description: '천장·창틀·외벽 모서리와 최근 보수 이유를 묻고 기록해야 해요.',
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
          <p className={styles.eyebrow}>집을 구하는 사람을 위한 · 매물의 기록과 관리</p>
          <h1 id="intro-heading">
            집은 짧게 보지만,
            <br />
            놓친 문제는 매일 반복됩니다.
            <br />
            <mark>돈을 잃지 않는 방을 고르세요.</mark>
          </h1>
          <p className={styles.heroDescription}>
            주소·사진·메모·체크 결과를 매물별로 기록하고 관리해, 관리비·보증금·수리비처럼 계약 뒤에 알면 늦는 돈을 집을
            보는 순간 확인하세요.
          </p>
          <p className={styles.heroPositioning}>
            <Icon name="arrow-right" size={17} />
            <span>
              계약을 성사시키는 사람은 있어도, 세입자가 떠안을 손해까지 먼저 알려주는 사람은 드뭅니다.
              <strong>자취선배는 계약 후를 살아갈 임차인의 편입니다.</strong>
            </span>
          </p>
          <div className={styles.heroActions}>
            <Link className={styles.primaryCta} to="/login">
              내 방에서 돈 새는 곳 확인하기
              <Icon name="arrow-right" size={18} />
            </Link>
            <a className={styles.secondaryCta} href="#money-loss">
              2분 만에 돈 새는 곳 보기
            </a>
          </div>
          <p className={styles.entryNote}>Google 로그인 없이 이름이나 닉네임만 입력하면 바로 사용할 수 있어요.</p>
        </div>

        <div className={styles.previewDeck} aria-label="자취선배의 매물 비용 비교와 기록 관리 예시">
          <article className={styles.preview} aria-label="월세만으로 비교했을 때 놓칠 수 있는 비용 예시">
            <div className={styles.previewHeader}>
              <span>손실 비교 · 후보 매물 A의 2년 총비용</span>
              <strong>역에서 5분 · 최상층 원룸</strong>
              <small>광고에서는 주변 매물보다 월세 5만원 저렴</small>
            </div>
            <div className={styles.previewSaving}>
              <span>보이는 월세</span>
              <strong>− 5만원</strong>
              <small>주변 매물보다 저렴</small>
            </div>
            <div className={styles.previewLosses}>
              <p>
                <span>관리비·별도 비용</span>
                <strong>+ 5만원 / 월</strong>
              </p>
              <p>
                <span>전입신고 가능 여부</span>
                <strong>확인 전</strong>
              </p>
              <p>
                <span>누수·방수 이력</span>
                <strong>확인 전</strong>
              </p>
              <p>
                <span>보증금 권리관계</span>
                <strong>확인 전</strong>
              </p>
            </div>
            <div className={styles.previewBottomLine}>
              <span>월 5만원 차이 × 24개월</span>
              <strong>2년이면 120만원</strong>
              <small>예시이며 실제 비용은 매물 조건에 따라 달라집니다.</small>
            </div>
          </article>

          <article
            className={`${styles.preview} ${styles.recordPreview}`}
            aria-label="사진과 메모, 체크리스트를 모은 매물 기록 관리 예시"
          >
            <div className={styles.previewHeader}>
              <span>매물의 기록과 관리 · 후보 매물 A</span>
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
          </article>
        </div>
      </section>

      <section className={styles.problem} id="money-loss" aria-labelledby="problem-heading">
        <p className={styles.sectionLabel}>싼 방의 계산서는 · 나중에 옵니다</p>
        <h2 id="problem-heading">집 구할 땐 다들 친절합니다. 손해는 그 다음부터 시작됩니다.</h2>
        <div className={styles.lossGrid} aria-label="계약 전에 확인할 금전 손실 가능성 예시">
          {lossSignals.map((signal, index) => (
            <article key={signal.title}>
              <span>LOSS {String(index + 1).padStart(2, '0')}</span>
              <strong>{signal.amount}</strong>
              <h3>{signal.title}</h3>
              <p>{signal.description}</p>
            </article>
          ))}
        </div>
        <p>
          겁을 주기 위한 숫자가 아닙니다. 계약 전에 질문하고 기록하면 비교할 수 있는 위험입니다. 금액과 발생 여부는 실제
          매물·계약 조건에 따라 달라집니다.
        </p>
      </section>

      <section className={styles.howToUse} id="how-to-use" aria-labelledby="how-heading">
        <p className={styles.sectionLabel}>매물의 기록과 관리 순서</p>
        <h2 id="how-heading">방을 등록하고, 돈이 새는 질문부터 확인하고, 마지막에 비교하세요.</h2>
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
        <p className={styles.sectionLabel}>자취선배가 임차인 편에서 돕는 방식</p>
        <h2 id="value-heading">방의 장점은 설명해줍니다. 위험은 내가 물어봐야 합니다.</h2>
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
          <p className={styles.sectionLabel}>누구의 기준으로 볼 것인가</p>
          <h2 id="audience-heading">계약을 성사시키는 기준과, 계약 후를 살아가는 기준은 다릅니다.</h2>
          <p className={styles.audienceDescription}>
            임대인은 집을 내놓고 공인중개사는 계약을 중개합니다. 각자의 역할이 있습니다. 자취선배는 누구를 의심하라고
            말하지 않고, 그 집에서 살고 돈을 낼 사람이 놓치기 쉬운 질문을 먼저 꺼냅니다.
          </p>
        </div>
        <div className={styles.roleStack}>
          <p>
            <span>임대인</span>
            집을 내놓는 역할
          </p>
          <p>
            <span>공인중개사</span>
            계약을 중개하는 역할
          </p>
          <p data-emphasis="true">
            <span>자취선배</span>
            계약 후를 살아갈 임차인의 기록 도구
          </p>
          <blockquote>
            집을 보여주는 사람은 많지만,
            <strong>자취선배는 그 집에서 살아갈 사람의 편입니다.</strong>
          </blockquote>
        </div>
      </section>

      <section className={styles.finalCta} aria-labelledby="final-cta-heading">
        <span className={styles.finalIcon}>
          <Icon name="home" size={28} />
        </span>
        <p>다음 방을 보기 전에</p>
        <h2 id="final-cta-heading">돈을 잃지 않는 방을 고르세요.</h2>
        <p className={styles.finalDescription}>
          가입 절차 없이 닉네임으로 시작하고, 다음 임장에서 필요한 질문과 답을 매물별로 기록하고 관리하세요.
        </p>
        <Link className={styles.primaryCta} to="/login">
          닉네임으로 무료 시작하기
          <Icon name="arrow-right" size={18} />
        </Link>
        <small className={styles.boundaryNote}>
          자취선배는 법률·지원 자격·하자 여부를 확정하거나 손해 방지를 보장하지 않습니다. 빠진 질문을 발견하고 저장한
          사실을 비교하도록 돕습니다.
        </small>
        <small>
          비밀번호 없이 만든 닉네임은 같은 이름을 입력한 사람과 기록을 공유합니다. 보호가 필요하면 시작할 때 선택
          비밀번호를 설정하세요.
        </small>
      </section>
    </div>

    <footer className={styles.footer}>
      <img src={logo} alt="" />
      <p>집을 보여주는 사람이 아니라, 그 집에서 살아갈 사람의 편.</p>
    </footer>
  </main>
);

export default IntroPage;
