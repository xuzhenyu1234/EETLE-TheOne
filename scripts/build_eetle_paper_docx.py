import html
import os
import zipfile


OUT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..",
                                   "EETLE_中文期刊论文初稿.docx"))


def esc(text):
    return html.escape(text, quote=True)


def p(text="", style=None, bold=False, align=None):
    ppr = ""
    if style:
        ppr += '<w:pStyle w:val="%s"/>' % esc(style)
    if align:
        ppr += '<w:jc w:val="%s"/>' % esc(align)
    if ppr:
        ppr = "<w:pPr>%s</w:pPr>" % ppr
    rpr = ""
    if bold:
        rpr = "<w:rPr><w:b/><w:bCs/></w:rPr>"
    return "<w:p>%s<w:r>%s<w:t xml:space=\"preserve\">%s</w:t></w:r></w:p>" % (
        ppr, rpr, esc(text))


def table(headers, rows):
    col_count = len(headers)
    xml = ['<w:tbl><w:tblPr><w:tblStyle w:val="TableGrid"/>'
           '<w:tblW w:w="0" w:type="auto"/></w:tblPr><w:tblGrid>']
    for _ in range(col_count):
        xml.append('<w:gridCol w:w="1800"/>')
    xml.append('</w:tblGrid>')
    xml.append(row(headers, header=True))
    for r in rows:
        xml.append(row(r, header=False))
    xml.append('</w:tbl>')
    return "".join(xml)


def row(values, header=False):
    cells = []
    for v in values:
        shading = '<w:shd w:fill="D9EAF7"/>' if header else ""
        b = '<w:b/><w:bCs/>' if header else ""
        cells.append(
            '<w:tc><w:tcPr><w:tcW w:w="1800" w:type="dxa"/>%s</w:tcPr>'
            '<w:p><w:pPr><w:jc w:val="center"/></w:pPr><w:r><w:rPr>%s</w:rPr>'
            '<w:t xml:space="preserve">%s</w:t></w:r></w:p></w:tc>' %
            (shading, b, esc(str(v))))
    return "<w:tr>%s</w:tr>" % "".join(cells)


def sect():
    return ('<w:sectPr><w:pgSz w:w="11906" w:h="16838"/>'
            '<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" '
            'w:left="1440" w:header="720" w:footer="720" w:gutter="0"/>'
            '</w:sectPr>')


parts = []

parts.append(p("面向灾后无人机蜂群网络的环境—事件协同动态信任管理与鲁棒 Leader 选举机制",
               "Title", True, "center"))
parts.append(p("摘要", "Heading1"))
parts.append(p(
    "灾后应急场景中，地面通信基础设施易受破坏，无人机蜂群网络可快速构建临时空中通信与协同感知系统，为道路阻断检测、危险区域识别、物资需求上报和救援通信中继提供支撑。然而，灾后无人机蜂群网络同时面临链路质量剧烈波动、节点观测不充分、任务事件真实性难以验证以及恶意节点攻击等问题。现有信任管理方法多依赖通信成功率或历史交互统计，容易将环境诱导的通信失败误判为节点恶意，也难以识别通信正常但事件语义虚假的攻击行为。针对上述问题，本文提出一种环境—事件协同动态信任管理与鲁棒 Leader 选举机制 EETLE。该机制将节点信任状态表示为可信度、环境不确定性、认知不确定性和不可信度四元向量，通过链路中断概率刻画通信环境影响，并设计环境感知信任恢复机制 EATR，以区分环境失败与恶意行为。同时，本文利用信任加权事件共识实现事件真实性一致性奖惩，识别虚假灾情上报；在全局信任阶段，引入基于人工权重的线性注意力融合方法，综合推荐节点自身可信度、推荐一致性、通信质量和空间相关性；在 Leader 选举阶段，融合全局信任、信任稳定性、通信质量、区域约束和切换裕度，提高蜂群协同控制的安全性与稳定性。仿真实验表明，EETLE 在恶意节点检测、正常节点误判抑制、虚假消息识别和 Leader 安全选举方面具有较好的综合性能。"))
parts.append(p("关键词：灾后应急通信；无人机蜂群网络；动态信任管理；环境不确定性；事件一致性；Leader 选举；恶意节点检测"))

sections = [
("1 引言",
["地震、洪涝、山体滑坡、泥石流以及城市建筑坍塌等灾后场景通常伴随地面基站损毁、道路交通受阻、灾情信息不完整和救援需求快速变化等问题。传统地面通信网络和人工巡检方式难以及时恢复灾区通信能力，也难以支撑大范围、动态化的灾情态势感知。无人机蜂群网络具有部署灵活、机动性强、覆盖范围广和协同感知能力强等特点，能够在灾后快速构建临时空中通信网络，并承担通信中继、道路阻断检测、危险区域识别、伤员位置发现和物资需求上报等任务，因此成为灾后应急通信与智能救援中的重要技术手段。",
"与一般移动自组织网络相比，灾后无人机蜂群网络面临更加复杂的安全与可信协同问题。一方面，无人机节点高速移动，拓扑结构随时间快速变化，节点间链路容易受到距离变化、建筑物遮挡、地形阻挡、烟尘干扰、噪声增强和临时电磁干扰等因素影响，导致通信质量显著波动。另一方面，灾后任务不仅关注数据能否成功传输，还关注节点上报的道路阻断、危险区域、物资需求等任务事件是否真实。换言之，灾后无人机蜂群中的信任管理不能仅依据通信成功率判断节点可信性，还需要同时考虑通信环境、节点行为和事件语义。",
"现有信任管理方法通常基于节点交互结果构建信任值，将成功交互视为可信证据，将失败交互视为不可信证据，并通过直接信任、推荐信任或全局声誉融合得到综合评价。这类方法具有结构简单、计算开销较低等优点，但在灾后无人机蜂群场景中存在明显局限。首先，通信失败并不必然意味着节点恶意。在复杂灾区环境下，链路中断、信号衰落和干扰增强均可能导致正常节点转发失败。若将所有失败交互直接计入不可信证据，容易造成正常节点误判。其次，部分恶意节点可能采用开关攻击策略，在正常阶段积累信任，在攻击阶段突然执行丢包、虚假上报或恶意推荐，从而绕过基于长期平均行为的检测机制。再次，恶意节点可能在通信链路正常的情况下上报虚假灾情信息，此时通信层表现正常，但任务语义层已经受到攻击，单纯依赖通信成功率难以及时识别。",
"针对上述问题，本文提出环境—事件协同动态信任管理与鲁棒 Leader 选举机制 EETLE。本文贡献包括：提出四元信任表示方法，区分可信度、环境不确定性、认知不确定性和不可信度；设计链路中断概率驱动的 EATR，降低复杂链路条件下正常节点被误判的概率；构建事件真实性一致性奖惩机制，识别通信正常但语义异常的虚假消息注入行为；提出人工权重线性注意力全局信任融合方法，抑制低可信推荐和跨区域低相关推荐；构建融合全局信任、稳定性、通信质量、区域约束和切换裕度的鲁棒 Leader 选举机制。"]),
("2 相关工作",
["2.1 无人机蜂群网络与灾后应急通信：无人机蜂群网络是飞行自组织网络的重要应用形态，适合用于灾后通信恢复、灾情侦察、临时中继、道路巡检和物资投送等任务[文献待补充]。现有研究主要围绕移动模型、链路稳定性、路由协议、覆盖优化、能耗控制和任务分配展开[文献待补充]，但较少同时考虑通信环境、事件真实性和恶意攻击下的安全信任管理。",
"2.2 移动自组织网络中的信任管理：直接信任、推荐信任和综合信任是自组织网络中常用的安全协作机制。Beta 声誉模型通过成功和失败交互估计节点可信度[文献待补充]。然而，传统方法通常默认失败交互主要由节点不合作或恶意行为导致，缺少对灾后复杂链路环境的显式建模。",
"2.3 无人机网络中的安全攻击：无人机蜂群网络易受到黑洞攻击、开关攻击、虚假消息注入攻击、恶意推荐攻击、环境隐蔽攻击和跨域攻击影响[文献待补充]。其中，环境隐蔽攻击利用链路恶化掩盖恶意行为，跨域攻击利用新区信任冷启动重新伪装，是灾后场景中具有代表性的复合威胁。",
"2.4 环境感知信任与事件一致性机制：已有环境感知信任方法通常将链路质量作为权重或上下文修正因子[文献待补充]，但较少明确区分环境不确定性和行为不可信度。事件一致性研究包括多数投票、异常检测和信任加权共识等[文献待补充]，但与通信行为信任的结合仍不充分。",
"2.5 推荐融合与 Leader 选举：分布式蜂群需要融合多节点推荐。简单平均易受恶意推荐影响，可学习注意力依赖训练数据且部署成本较高。Leader 选举研究多关注能量、距离、连通性或负载[文献待补充]，对开关攻击、跨域攻击和虚假推荐考虑不足。本文采用人工权重线性注意力和多约束 Leader 评分，以提高在线部署可解释性和鲁棒性。"]),
("3 系统模型与问题定义",
["设灾后无人机蜂群由节点集合 V={v1,v2,...,vN} 构成，节点 vi 在时刻 t 的位置为 pos_i(t)，节点间距离为 dist_ij(t)。若两节点处于通信范围内且链路可用，则可以建立临时连接并执行消息转发、事件上报或信任交换。每个节点维护本地信任表，Leader 周期性收集本地信任信息并进行全局融合与重选判断。",
"实验采用逻辑双区域灾后场景。仿真区域大小为 1000 m × 1000 m，以 x=500 m 为边界划分为两个灾后任务区域。节点根据当前位置动态判定所属区域，并通过 homeRegion 与 currentRegion 标识是否处于跨域状态。跨域攻击节点在设定时间后迁移至另一逻辑区域，系统据此触发区域折扣、跨域暖启动和 Leader 候选约束。",
"链路环境模型从接收功率、SINR、信道容量和链路中断概率出发。接收功率为 P_r,ij(t)=P_t*g_ij(t)，路径增益为 g_ij(t)=1/(dist_ij(t)^alpha+epsilon)。链路信干噪比为 SINR_ij(t)=P_r,ij(t)/(N0+I_ij(t))，信道容量为 C_ij(t)=B log2(1+SINR_ij(t))。当 C_ij(t)<R_req，或等价地 SINR_ij(t)<gamma_th=2^(R_req/B)-1 时，认为链路处于中断状态。链路中断概率 p_out,ij(t) 由滑动统计估计，并用于环境不确定性建模。",
"交互行为模型记录节点 vi 对 vj 的成功交互次数 s_ij(t) 和失败交互次数 f_ij(t)。成功交互包括消息成功转发和任务数据成功提交，失败交互包括转发失败、传输中断或应转发而未转发。任务事件集合记为 E={e1,e2,...,eK}，事件真实状态 x_k(t) 与节点报告 r_j,k(t) 均取 0 或 1。",
"本文考虑黑洞攻击、开关攻击、虚假消息注入攻击、环境隐蔽攻击、跨域攻击和恶意推荐攻击。研究目标是在复杂链路环境和多类型攻击下，在线计算节点本地信任与全局信任，并选出可信、稳定、通信质量较好且满足区域约束的 Leader。"]),
("4 环境—事件协同动态信任管理机制",
["EETLE 的本地信任管理采用环境—事件协同更新框架。每个节点根据直接交互结果更新行为证据，根据链路中断概率估计环境不确定性，根据观测次数计算认知不确定性，并通过四元信任向量表示节点可信状态。随后，EATR 根据 p_out 将部分失败风险归因于环境因素；环境隐蔽攻击修正机制识别长期集中在高环境中断阶段的异常行为；事件一致性奖惩机制从任务语义层校正节点信任。",
"设节点 vi 对 vj 的成功交互次数为 s_ij，失败交互次数为 f_ij。采用 Beta 平滑得到 b^0_ij=(s_ij+1)/(s_ij+f_ij+2)，d^0_ij=(f_ij+1)/(s_ij+f_ij+2)。其中 b^0_ij 表示由成功交互支持的初始可信质量，d^0_ij 表示由失败交互支持的初始不可信质量。平滑项可避免冷启动阶段因少量观测导致信任值过度极端。",
"认知不确定性定义为 u^0_ij=beta_cog/(beta_cog+s_ij+f_ij)。当交互次数较少时，u^0_ij 较高；随着观测次数增加，认知不确定性逐渐降低。环境不确定性由链路中断概率映射得到 e^0_ij=alpha_env p_out,ij，其中 alpha_env 为环境影响系数。该公式表明环境不确定性由链路状态推导得到，而非人工固定赋值。",
"节点 vi 对 vj 的本地信任表示为四元向量 T_ij(t)=<b_ij(t), e_ij(t), u_ij(t), d_ij(t)>，分别表示可信度、环境不确定性、认知不确定性和不可信度，满足 b_ij+e_ij+u_ij+d_ij=1。令 Z=b^0_ij+e^0_ij+u^0_ij+d^0_ij，则 b_ij=b^0_ij/Z，e_ij=e^0_ij/Z，u_ij=u^0_ij/Z，d_ij=d^0_ij/Z。",
"EATR 定义环境驱动的不可信质量 d^env_ij=eta_env p_out,ij d_ij，并更新 d'_ij=d_ij-d^env_ij，e'_ij=e_ij+d^env_ij，b'_ij=b_ij，u'_ij=u_ij。当 p_out 较高时，更多失败风险被解释为环境因素；当 p_out 较低时，失败主要保留为不可信证据。",
"为防止环境隐蔽攻击，定义高环境失败集中度 H_ij=F^high_ij/N^high_ij。当 H_ij 超过风险阈值 theta_h 时，将部分环境不确定性和认知不确定性转移至不可信度：Delta e=rho_h risk_ij e_ij，Delta u=rho_h risk_ij u_ij，e'_ij=e_ij-Delta e，u'_ij=u_ij-Delta u，d'_ij=d_ij+Delta e+Delta u。",
"进一步地，为在检测阶段刻画节点是否存在利用恶劣环境隐藏攻击的行为，本文定义环境隐蔽风险 Risk_j。对于目标节点 v_j，统计其在高链路中断条件下的失败次数 F^high_j、高链路中断条件下的总交互次数 N^high_j 以及总失败次数 F^total_j。环境隐蔽风险定义为 Risk_j=max(F^high_j/F^total_j, F^high_j/N^high_j)。其中，第一项表示节点失败是否集中发生在恶劣环境下，第二项表示节点在恶劣环境下一旦交互是否经常失败。若 Risk_j 较高，说明该节点可能选择在高 p_out 条件下发动攻击，从而将恶意失败伪装为环境失败。",
"基于上述风险度量，本文在恶意节点检测阶段不只依赖全局信任阈值，而采用联合判定规则：若 GT_j < theta_T 或 Risk_j > theta_R，且环境风险证据数量达到最小样本要求，则将节点 v_j 标记为可疑节点。该规则使 EETLE 能够识别全局信任值仍然较高但失败行为高度集中于恶劣环境的环境隐蔽攻击节点。",
"事件一致性机制利用信任加权邻居共识计算事件发生概率 P_k(t)=sum_{vm in N_i} LT_im(t) r_m,k(t) / sum_{vm in N_i} LT_im(t)。当节点报告与可信邻居共识一致时，将部分认知不确定性转移为可信度；当报告与共识相反时，将部分认知不确定性转移为不可信度。该机制用于识别通信正常但事件语义异常的虚假消息注入攻击。",
"本文采用的本地标量信任计算公式为 LT_ij(t)=[T_ij(t)+alpha_C C_ij(t)+alpha_E E_ij(t)]/[T_ij(t)+alpha_C C_ij(t)+alpha_E E_ij(t)+alpha_D D_ij(t)]，其中 T_ij、C_ij、E_ij 和 D_ij 分别对应可信度、认知不确定性、环境不确定性和不可信度。参数满足 0 <= alpha_E <= alpha_C <= 1，alpha_D >= 1。该公式允许认知不确定性和环境不确定性以谨慎方式贡献到信任分子，同时保留不可信度在分母中的显式惩罚作用。"]),
("5 基于线性注意力融合的全局信任计算与鲁棒 Leader 选举机制",
["Leader 收集多个节点对目标节点的本地信任评价。设目标节点为 vj，推荐节点集合为 R_j(t)，推荐节点 vi 提供本地标量信任 LT_ij(t)，则全局信任为 GT_j(t)=sum_{vi in R_j(t)} alpha_ij(t) LT_ij(t)，其中推荐权重 alpha_ij(t) 非负且和为 1。",
"推荐节点自身可信度采用上一轮全局信任 R_i(t)=GT_i(t-1)，初始化阶段设置为 0.5。推荐一致性定义为 A_i(t)=1-(1/|H_i|) sum_{vj in H_i} |LT_ij(t)-GT_j(t-1)|，用于衡量推荐节点历史评价与全局结果之间的偏差。通信质量定义为 Q_i(t)=1-p_out,iL(t)，空间相关性定义为 S_ij(t)=exp(-dist_ij(t)/sigma_S)，跨区域推荐进一步乘以区域折扣因子。",
"考虑灾后场景训练样本有限、网络状态快速变化，本文不采用需要大量训练数据的可学习注意力，而采用人工权重线性注意力。推荐得分为 score_ij(t)=omega_R R_i(t)+omega_A A_i(t)+omega_Q Q_i(t)+omega_S S'_ij(t)，其中权重和为 1。通过 Softmax 得到推荐权重 alpha_ij(t)。该方法参数含义明确、计算复杂度低，适合在线部署。",
"Leader 候选集合为 C(t)={vj | GT_j(t)>=theta_C}。对于跨域节点，若其在当前区域内有效交互次数低于阈值，则暂不允许进入 Leader 候选集合。信任稳定性定义为 ST_j(t)=1/(1+Var(GT_j))，通信质量定义为候选节点与邻居链路质量的平均值 CQ_j(t)=sum_{vm in N_j(t)}(1-p_out,jm(t))/|N_j(t)|。",
"Leader 基础评分为 Score_j(t)=phi_T GT_j(t)+phi_S ST_j(t)+phi_Q CQ_j(t)，其中 phi_T+phi_S+phi_Q=1。区域约束后的最终评分为 ScoreFinal_j(t)=RF_j(t) Score_j(t)，最终 Leader 为评分最高的候选节点。为避免频繁切换，仅当新候选评分超过当前 Leader 评分并大于切换裕度时才执行切换；当当前 Leader 全局信任、稳定性或通信质量低于异常阈值时触发重选。"]),
("6 理论性质与复杂度分析",
["EETLE 将本地信任表示为四元向量，并通过归一化保证 b_ij+e_ij+u_ij+d_ij=1。EATR、环境隐蔽攻击修正和事件一致性奖惩均属于四元质量内部转移，因此不会改变总质量，满足信任质量守恒性。",
"本地标量信任公式中，分子和分母均非负，且分母比分子多出 alpha_D D_ij 项，因此 LT_ij 位于 [0,1]。全局信任是多个本地信任的凸组合，也位于 [0,1]。该有界性保证信任值可用于阈值判断和 Leader 评分。",
"本地信任映射关于可信度单调增加，关于不可信度单调降低。由于 alpha_D >= 1，不可信度对分母具有明确惩罚作用；认知不确定性和环境不确定性以 alpha_C 和 alpha_E 加权进入分子，使模型在不确定环境下保持保守但不过度悲观的评分特征。",
"EATR 能够在高 p_out 场景下将部分失败风险归因于环境因素，降低正常节点误判；在低 p_out 场景下，失败仍主要保留为不可信证据，保持对恶意行为的惩罚能力。环境隐蔽攻击修正机制则通过高环境失败集中度识别长期利用恶劣环境掩盖攻击的节点，与 EATR 形成互补。",
"事件一致性机制通过信任加权邻居共识补充通信行为证据，使模型能够识别通信成功但事件内容虚假的节点。线性注意力融合根据推荐者自身可信度、推荐一致性、通信质量和空间相关性分配推荐权重，降低恶意推荐影响。Leader 选举进一步通过全局信任、稳定性、通信质量和区域约束降低恶意节点获得控制权的概率。",
"设节点数为 N，当前通信边数为 M，事件数量为 K，平均邻居数为 d_bar，平均推荐节点数为 r_bar。本地信任更新复杂度为 O(M)，事件一致性复杂度约为 O(K d_bar)，全局信任融合复杂度为 O(N r_bar)，Leader 选举复杂度约为 O(N+|C|d_bar)。由于无人机通信范围有限，网络通常为稀疏图，因此 EETLE 适用于灾后中小规模无人机蜂群在线信任管理。"])
]

for title, paras in sections:
    parts.append(p(title, "Heading1"))
    for para in paras:
        parts.append(p(para))

parts.append(p("7 仿真实验与结果分析", "Heading1"))
parts.append(p("7.1 实验设置", "Heading2"))
parts.append(p("本文基于 The ONE 1.6.0 仿真平台构建灾后无人机蜂群网络实验场景。仿真区域大小为 1000 m × 1000 m，采用逻辑双区域划分，以 x=500 m 为区域边界。网络共设置 60 个无人机节点，节点采用 RandomWaypoint 移动模型，移动速度为 5-20 m/s，等待时间为 0-10 s。通信接口传输范围为 500 m，传输速率为 250 kbit/s，节点缓存为 5 MB，消息 TTL 为 300 s。仿真总时长为 10000 s，更新间隔为 10 s，统计间隔为 100 s。"))
parts.append(p("攻击模型包含黑洞攻击、开关攻击、虚假消息注入攻击、环境隐蔽攻击和跨域攻击。在恶意节点比例实验中，攻击比例分别设置为 0%、10%、20%、30% 和 40%。当恶意节点比例为 30% 时，60 个节点中包含 18 个恶意节点和 42 个正常节点。"))
parts.append(p("7.2 对比方法与消融设置", "Heading2"))
parts.append(p("实验设置完整模型 EETLE-Full，以及 No-EATR、No-Event、No-Attention、No-Region、No-Stability 和 No-Switch-Margin 等消融模型，用于评估环境归因、事件一致性、线性注意力、区域约束、信任稳定性和切换裕度对总体性能的影响。"))
parts.append(p("7.3 恶意节点检测性能", "Heading2"))
parts.append(table(["恶意比例", "恶意节点数", "Precision", "Recall", "F1", "FPR", "TP", "FP", "FN", "TN"],
                   [["0%", 0, "NA", "NA", "NA", "0.0000", 0, 0, 0, 60],
                    ["10%", 6, "1.0000", "1.0000", "1.0000", "0.0000", 6, 0, 0, 54],
                    ["20%", 12, "1.0000", "1.0000", "1.0000", "0.0000", 12, 0, 0, 48],
                    ["30%", 18, "1.0000", "1.0000", "1.0000", "0.0000", 18, 0, 0, 42],
                    ["40%", 24, "1.0000", "0.9167", "0.9565", "0.0000", 22, 0, 2, 36]]))
parts.append(p("结果显示，在恶意节点比例为 10%、20% 和 30% 时，EETLE 的 Precision、Recall 和 F1 均达到 1.0000，FPR 为 0。在 40% 恶意节点比例下，Precision 仍保持 1.0000，FPR 仍为 0，但 Recall 降至 0.9167，说明高恶意密度下仍存在少量漏检，但正常节点未被误判。"))
parts.append(p("7.4 信任区分能力分析", "Heading2"))
parts.append(table(["恶意比例", "正常平均GT", "恶意平均GT", "黑洞", "开关", "虚假事件", "环境隐蔽", "跨域"],
                   [["10%", "0.7391", "0.3510", "0.3846", "0.1204", "0.0789", "0.7385", "0.3990"],
                    ["20%", "0.7378", "0.3321", "0.3900", "0.1291", "0.0864", "0.7289", "0.3988"],
                    ["30%", "0.7401", "0.3391", "0.3951", "0.1370", "0.1303", "0.7555", "0.3959"],
                    ["40%", "0.7456", "0.3740", "0.3971", "0.1425", "0.1588", "0.7865", "0.3880"]]))
parts.append(p("正常节点平均全局信任稳定保持在 0.7378-0.7456 区间，而恶意节点平均全局信任处于 0.3321-0.3740 区间，二者形成明显区分。需要说明的是，EETLE 的全局信任值不是节点正常概率，而是同时考虑可信度、环境不确定性、认知不确定性和不可信度后的保守型安全评分。因此，在灾后链路波动和观测不充分条件下，正常节点信任值不必趋近于 1。实验中正常节点与恶意节点之间的信任间隔和 0 误判率更能体现模型有效性。"))
parts.append(p("环境隐蔽攻击节点在部分实验轮次中仍保持较高的平均全局信任值，这并不意味着模型无法识别该类攻击，而是与 EETLE 的环境归因设计有关。本文的本地信任标量映射采用 LT_ij=(T_ij+alpha_C C_ij+alpha_E E_ij)/(T_ij+alpha_C C_ij+alpha_E E_ij+alpha_D D_ij)。其中，E_ij 表示环境不确定性，D_ij 表示不可信度。与 D_ij 不同，E_ij 表示交互失败可能由链路中断、干扰增强或遮挡等环境因素导致，因此不应被直接视为恶意证据。特别是在灾后复杂链路环境中，若将高 p_out 条件下的所有失败均计入不可信度，会显著提高正常节点的误判率。为此，EETLE 通过 EATR 机制将部分不可信质量由 D_ij 转移至 E_ij，从而实现环境失败与恶意失败的概率区分。"))
parts.append(p("然而，环境隐蔽攻击节点正是利用这一特点，在高链路中断概率条件下集中发动攻击，使其部分恶意失败被归入环境不确定性。由于环境不确定性在标量信任映射中以 alpha_E E_ij 的形式进入分子，因此仅从全局信任 GT 观察时，环境隐蔽攻击节点的信任下降并不一定像黑洞攻击或虚假消息注入攻击那样明显。该现象说明，环境隐蔽攻击具有较强的信任伪装性，不能仅依赖单一 GT 阈值进行判断。"))
parts.append(p("因此，EETLE 对环境隐蔽攻击采用联合检测策略，即在全局信任评估之外进一步引入高环境失败集中度和环境风险指标。当节点长期在高 p_out 条件下出现异常失败时，环境隐蔽攻击风险逐渐升高，模型会将部分环境不确定性或认知不确定性重新转移至不可信度，并在检测阶段采用判定规则 GT_j < theta_T 或 Risk_j > theta_R。也就是说，节点只要满足低全局信任或高环境风险中的任一条件，即可被判定为可疑节点。该设计既避免了将正常节点在恶劣链路下的偶发失败误判为恶意行为，又能够识别长期利用环境异常隐藏攻击的节点。"))
parts.append(p("在本组实验末期，环境隐蔽攻击节点 52-55 的全局信任分别约为 0.7516、0.7442、0.7480 和 0.7655，单从 GT 观察并不低；但其环境隐蔽风险均达到 1.0000，且总失败证据分别为 3、5、7 和 6，满足环境风险检测阈值与最小证据要求。因此，这些节点最终均被标记为 predictedMalicious=true。该结果说明，环境隐蔽攻击节点的识别并不依赖其 GT 必须下降到较低水平，而是依赖“全局信任 + 环境风险”的联合判定。换言之，较高的环境隐蔽攻击 GT 反映了该攻击类型的伪装性，而 Risk 指标刻画了其失败行为在高 p_out 条件下的异常集中性。"))
parts.append(p("7.5 网络传输性能与 Leader 安全选举", "Heading2"))
parts.append(table(["恶意比例", "投递率", "开销比", "平均时延", "Leader切换", "恶意Leader", "正常Leader比例", "最终Leader类型"],
                   [["0%", "1.0000", "58.8466", "20.2861", 44, 0, "1.0000", "NORMAL"],
                    ["10%", "0.9941", "56.1306", "20.7656", 41, 2, "0.9800", "NORMAL"],
                    ["20%", "1.0000", "52.8702", "20.1652", 48, 2, "0.9800", "NORMAL"],
                    ["30%", "1.0000", "51.7522", "21.4985", 44, 1, "0.9900", "NORMAL"],
                    ["40%", "1.0000", "50.8909", "20.4277", 39, 19, "0.8100", "NORMAL"]]))
parts.append(p("在 10%-40% 恶意节点比例下，EETLE 的消息投递率均接近或达到 1.0000，平均时延维持在约 20-21.5 s 范围内。Leader 选举方面，在 0%-30% 恶意节点比例下，正常 Leader 比例不低于 0.98，最终 Leader 均为正常节点；当恶意比例为 40% 时，正常 Leader 比例下降至 0.81，但最终 Leader 仍为正常节点。"))
parts.append(p("7.6 事件一致性与消融实验", "Heading2"))
parts.append(table(["模型", "NORMAL", "BLACKHOLE", "ON_OFF", "FALSE_EVENT", "ENV_CAMOUFLAGE", "CROSS_REGION", "投递率", "恶意Leader", "正常Leader比例"],
                   [["Full", "0.7484", "0.4000", "0.1480", "0.1170", "0.7535", "0.4066", "1.0000", 4, "0.6000"],
                    ["No-EATR", "0.7439", "0.3964", "0.1373", "0.1378", "0.6895", "0.3916", "0.9971", 0, "1.0000"],
                    ["No-Event", "0.7431", "0.4012", "0.3947", "0.7400", "0.8160", "0.4070", "1.0000", 9, "0.1000"],
                    ["No-Attention", "0.7571", "0.4011", "0.1613", "0.1523", "0.7542", "0.3981", "1.0000", 0, "1.0000"],
                    ["No-Region", "0.7471", "0.3973", "0.1517", "0.1136", "0.7422", "0.3978", "1.0000", 0, "1.0000"],
                    ["No-Switch", "0.7502", "0.3977", "0.1346", "0.1168", "0.7511", "0.4009", "1.0000", 0, "1.0000"]]))
parts.append(p("消融结果显示，事件一致性机制对虚假消息注入攻击识别具有决定性影响。完整模型中 FALSE_EVENT 节点平均全局信任为 0.1170；去除事件一致性后，该值升高至 0.7400，接近正常节点信任水平。事件报告日志也显示，当节点 50 在 t=130 s 上报事件状态 1，而真实状态和共识状态均为 0 时，系统将其识别为 falseReport 并施加 0.2000 的惩罚；正常节点上报与共识一致时获得 0.0300 的奖励。"))
parts.append(p("8 结论", "Heading1"))
parts.append(p("本文面向灾后无人机蜂群网络中复杂链路环境、任务事件真实性和恶意攻击交织的问题，提出了环境—事件协同动态信任管理与鲁棒 Leader 选举机制 EETLE。该机制通过四元信任向量刻画可信度、环境不确定性、认知不确定性和不可信度，利用链路中断概率建立环境不确定性，并通过 EATR 将环境诱导失败与节点恶意行为进行概率区分。在任务语义层，EETLE 通过信任加权事件共识对虚假消息注入行为进行动态奖惩；在全局信任层，采用人工权重线性注意力融合多节点推荐；在 Leader 选举层，综合全局信任、信任稳定性、通信质量、区域约束和切换裕度，提高蜂群协同控制的安全性和稳定性。仿真实验表明，EETLE 能够在多种恶意比例下保持较高检测性能和较低误判率，并在中等恶意比例下维持较高正常 Leader 比例。未来工作将进一步面向真实无人机平台、多区域大规模场景和参数自适应优化开展研究。"))
parts.append(p("参考文献", "Heading1"))
for i in range(1, 8):
    parts.append(p("[%d] [参考文献待补充]" % i))


document_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>%s%s</w:body></w:document>''' % ("".join(parts), sect())

styles_xml = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:style w:type="paragraph" w:default="1" w:styleId="Normal">
<w:name w:val="Normal"/><w:qFormat/><w:pPr><w:spacing w:after="120" w:line="360" w:lineRule="auto"/></w:pPr>
<w:rPr><w:rFonts w:ascii="Times New Roman" w:eastAsia="宋体" w:hAnsi="Times New Roman" w:cs="Times New Roman"/><w:sz w:val="21"/><w:szCs w:val="21"/></w:rPr>
</w:style>
<w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/><w:qFormat/>
<w:pPr><w:jc w:val="center"/><w:spacing w:after="240"/></w:pPr>
<w:rPr><w:b/><w:bCs/><w:rFonts w:ascii="Times New Roman" w:eastAsia="黑体" w:hAnsi="Times New Roman"/><w:sz w:val="32"/><w:szCs w:val="32"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/>
<w:pPr><w:spacing w:before="240" w:after="120"/></w:pPr>
<w:rPr><w:b/><w:bCs/><w:rFonts w:ascii="Times New Roman" w:eastAsia="黑体" w:hAnsi="Times New Roman"/><w:sz w:val="28"/><w:szCs w:val="28"/></w:rPr></w:style>
<w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/><w:basedOn w:val="Normal"/><w:next w:val="Normal"/><w:qFormat/>
<w:pPr><w:spacing w:before="180" w:after="80"/></w:pPr>
<w:rPr><w:b/><w:bCs/><w:rFonts w:ascii="Times New Roman" w:eastAsia="黑体" w:hAnsi="Times New Roman"/><w:sz w:val="24"/><w:szCs w:val="24"/></w:rPr></w:style>
<w:style w:type="table" w:styleId="TableGrid"><w:name w:val="Table Grid"/><w:basedOn w:val="TableNormal"/><w:uiPriority w:val="59"/><w:qFormat/>
<w:tblPr><w:tblBorders><w:top w:val="single" w:sz="4" w:space="0" w:color="666666"/><w:left w:val="single" w:sz="4" w:space="0" w:color="666666"/><w:bottom w:val="single" w:sz="4" w:space="0" w:color="666666"/><w:right w:val="single" w:sz="4" w:space="0" w:color="666666"/><w:insideH w:val="single" w:sz="4" w:space="0" w:color="999999"/><w:insideV w:val="single" w:sz="4" w:space="0" w:color="999999"/></w:tblBorders><w:tblCellMar><w:top w:w="80" w:type="dxa"/><w:left w:w="80" w:type="dxa"/><w:bottom w:w="80" w:type="dxa"/><w:right w:w="80" w:type="dxa"/></w:tblCellMar></w:tblPr></w:style>
</w:styles>'''

content_types = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
</Types>'''

rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>'''

doc_rels = '''<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>'''

with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("[Content_Types].xml", content_types)
    z.writestr("_rels/.rels", rels)
    z.writestr("word/_rels/document.xml.rels", doc_rels)
    z.writestr("word/document.xml", document_xml)
    z.writestr("word/styles.xml", styles_xml)

print(OUT)
